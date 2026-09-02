package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.semaphore.domain.CourseStatus;
import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumCourse;
import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgressCourse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The one place that decides whether a pensum item's prerequisites are met.
 *
 * <p>This is the computation the "semaforo" is named for: a {@code PENDING} item is
 * eligible when every one of its prerequisites is {@link CourseStatus#PASSED}, and
 * <strong>blocked</strong> otherwise. {@code IN_PROGRESS} is deliberately not enough -
 * a course being taken this period has not settled, so it cannot unlock what depends on
 * it. Reusing this walker for both {@code GET /api/semaphore/me/eligible} and its own
 * unit tests is what keeps that rule in exactly one place instead of being re-derived
 * (and possibly re-broken) at each call site.
 *
 * <p>Prerequisites are course {@code code}s, never {@code pensumItemCode}s - an elective
 * slot cannot be required, since its content is not fixed - so the lookup is keyed by
 * {@link StudentProgressCourse#code()} via {@code StudentProgress#byCourseCode()}.
 */
@Component
public class PrerequisiteWalker {

    /**
     * @param item          the pensum item being evaluated for eligibility
     * @param byCourseCode  the student's own entries, keyed by course {@code code}
     * @return true when {@code item} has no prerequisites, or every prerequisite entry
     *         is present and {@link CourseStatus#PASSED}
     */
    public boolean allPrerequisitesPassed(CurriculumCourse item, Map<String, StudentProgressCourse> byCourseCode) {
        return item.prerequisites().stream().allMatch(code -> {
            StudentProgressCourse entry = byCourseCode.get(code);
            return entry != null && entry.status() == CourseStatus.PASSED;
        });
    }
}
