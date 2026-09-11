package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.semaphore.domain.Pensum;
import co.edu.konradlorenz.kapp.semaphore.domain.PensumCourse;
import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgress;
import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgressCourse;
import co.edu.konradlorenz.kapp.semaphore.repository.StudentProgressRepository;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ReconciliationDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Brings a pinned progress document back in step with the pensum it names, and
 * reports exactly what moved.
 *
 * <p>Nothing is ever silently dropped or invented: an item the pensum gained is
 * materialised as {@code PENDING} and named in {@code addedCourses}; an item the
 * document has that the pensum no longer declares is <strong>kept</strong> - a
 * passed grade is never destroyed - and named in {@code removedCourses}. This is the
 * one place that walks both collections, so {@code GET /api/semaphore/me} and every
 * mutating {@code /me/**} endpoint agree on what the student's semaforo currently
 * contains.
 *
 * <p>Runs on every read, but writes only when something actually diverged: an unchanged
 * document is returned as-is; there is no reason to bump its {@code updatedAt} for a
 * comparison that found nothing new.
 */
@Component
public class PensumReconciler {

    private final StudentProgressRepository progressRepository;

    public PensumReconciler(StudentProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    /**
     * @param progress   as pinned; already lazily created
     * @param pensum the pensum {@code progress.pensumCode()} names
     */
    public Result reconcile(StudentProgress progress, Pensum pensum) {
        Map<String, StudentProgressCourse> existingByItem = progress.byPensumItemCode();
        Map<String, PensumCourse> pensumByItem = pensum.byPensumItemCode();

        List<StudentProgressCourse> merged = new ArrayList<>();
        List<String> added = new ArrayList<>();
        for (PensumCourse item : pensum.coursesInDisplayOrder()) {
            StudentProgressCourse existing = existingByItem.get(item.pensumItemCode());
            if (existing != null) {
                merged.add(existing);
            } else {
                merged.add(StudentProgressCourse.pendingFor(item));
                added.add(item.addressableCode());
            }
        }

        List<String> removed = new ArrayList<>();
        for (StudentProgressCourse existing : progress.courses()) {
            if (!pensumByItem.containsKey(existing.pensumItemCode())) {
                merged.add(existing);
                removed.add(existing.addressableCode());
            }
        }

        if (added.isEmpty() && removed.isEmpty()) {
            return new Result(progress, ReconciliationDto.noDivergence());
        }

        StudentProgress persisted = progressRepository.save(progress.withCourses(merged));
        return new Result(persisted, ReconciliationDto.of(added, removed));
    }

    public record Result(StudentProgress progress, ReconciliationDto reconciliation) {
    }
}
