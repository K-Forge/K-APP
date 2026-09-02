package co.edu.konradlorenz.kapp.semaphore.web.dto;

import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgress;

import java.time.Instant;
import java.util.List;

/**
 * The student's own read: the semaforo plus how it compares with its curriculum.
 *
 * <p>Flattened rather than nested, because the contract declares it as an {@code allOf}
 * of {@code StudentProgress} and a {@code reconciliation} property - one object, not two.
 */
public record StudentProgressWithReconciliationDto(
        String userId,
        String studentCode,
        String programCode,
        String pensumCode,
        int currentLevel,
        List<StudentProgressCourseDto> courses,
        Instant updatedAt,
        ReconciliationDto reconciliation
) {

    public static StudentProgressWithReconciliationDto of(StudentProgress progress,
                                                          ReconciliationDto reconciliation) {
        return new StudentProgressWithReconciliationDto(
                progress.userId(), progress.studentCode(), progress.programCode(),
                progress.pensumCode(), progress.currentLevel(),
                progress.courses().stream().map(StudentProgressCourseDto::from).toList(),
                progress.updatedAt(), reconciliation);
    }
}
