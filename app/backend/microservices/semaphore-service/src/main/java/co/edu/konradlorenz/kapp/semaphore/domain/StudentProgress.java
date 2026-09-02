package co.edu.konradlorenz.kapp.semaphore.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A student's semaforo: one entry per item of the pinned pensum.
 *
 * <h2>The pensum is pinned, not followed</h2>
 * {@code pensumCode} records the curriculum this document was created from and does not
 * track later edits of it on its own. A student who enrolled under one plan is assessed
 * against that plan; silently re-pointing them at an edited curriculum would change what
 * they owe for their degree without anyone deciding to. Divergence is instead surfaced on
 * every read of {@code GET /api/semaphore/me} as a reconciliation block.
 *
 * <h2>userId is a String, not a long</h2>
 * It is the JWT {@code sub} claim, which auth-service mints as the user-service profile
 * id - a UUID. The semaphore OpenAPI document types this field as {@code integer/int64};
 * that is a contradiction with the user OpenAPI document, which types the same identifier
 * as a UUID string. See {@code NEEDS_VERIFICATION.md}: it cannot be resolved here without
 * editing a published contract, and a UUID does not fit in a long.
 *
 * @param id           Mongo document id
 * @param userId       the JWT subject of the student who owns this semaforo
 * @param studentCode  institutional student code
 * @param programCode  the student's program
 * @param pensumCode   the curriculum this document is pinned to
 * @param currentLevel the level the student is enrolled in
 * @param courses      one entry per pinned pensum item, plus entries retained from items
 *                     that the curriculum has since dropped
 * @param updatedAt    when the document was last written
 */
@Document(collection = "studentProgress")
public record StudentProgress(
        @Id String id,
        String userId,
        String studentCode,
        String programCode,
        String pensumCode,
        int currentLevel,
        List<StudentProgressCourse> courses,
        Instant updatedAt
) {

    public StudentProgress {
        courses = courses == null ? List.of() : List.copyOf(courses);
    }

    /**
     * Materialises a fresh semaforo: every item of the curriculum as PENDING.
     */
    public static StudentProgress materialise(String userId, String studentCode,
                                              Curriculum curriculum, int currentLevel) {
        List<StudentProgressCourse> entries = curriculum.coursesInDisplayOrder().stream()
                .map(StudentProgressCourse::pendingFor)
                .toList();
        return new StudentProgress(null, userId, studentCode, curriculum.programCode(),
                curriculum.pensumCode(), currentLevel, entries, Instant.now());
    }

    public Optional<StudentProgressCourse> findByAddressableCode(String identifier) {
        return courses.stream()
                .filter(c -> c.addressableCode().equals(identifier))
                .findFirst();
    }

    public Optional<StudentProgressCourse> findByPensumItemCode(String pensumItemCode) {
        return courses.stream()
                .filter(c -> c.pensumItemCode().equals(pensumItemCode))
                .findFirst();
    }

    public Map<String, StudentProgressCourse> byPensumItemCode() {
        Map<String, StudentProgressCourse> index = new LinkedHashMap<>();
        courses.forEach(c -> index.put(c.pensumItemCode(), c));
        return index;
    }

    /** @return a copy with {@code replacement} substituted for the entry it shares a pensum item with */
    public StudentProgress withCourse(StudentProgressCourse replacement) {
        List<StudentProgressCourse> updated = new ArrayList<>(courses.size());
        for (StudentProgressCourse existing : courses) {
            updated.add(existing.pensumItemCode().equals(replacement.pensumItemCode())
                    ? replacement
                    : existing);
        }
        return new StudentProgress(id, userId, studentCode, programCode, pensumCode,
                currentLevel, updated, Instant.now());
    }

    public StudentProgress withCourses(List<StudentProgressCourse> replacement) {
        return new StudentProgress(id, userId, studentCode, programCode, pensumCode,
                currentLevel, replacement, Instant.now());
    }
}
