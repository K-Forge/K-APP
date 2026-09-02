package co.edu.konradlorenz.kapp.semaphore.web.dto;

import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgress;

import java.time.Instant;
import java.util.List;

/**
 * A student's semaforo without the reconciliation block - the administrative read.
 *
 * <p>{@code userId} is a String here because it is the JWT {@code sub}, a UUID minted by
 * user-service. The published semaphore contract types it as {@code integer/int64}, which
 * contradicts the user contract. See {@code NEEDS_VERIFICATION.md}.
 */
public record StudentProgressDto(
        String userId,
        String studentCode,
        String programCode,
        String pensumCode,
        int currentLevel,
        List<StudentProgressCourseDto> courses,
        Instant updatedAt
) {

    public static StudentProgressDto from(StudentProgress progress) {
        return new StudentProgressDto(
                progress.userId(), progress.studentCode(), progress.programCode(),
                progress.pensumCode(), progress.currentLevel(),
                progress.courses().stream().map(StudentProgressCourseDto::from).toList(),
                progress.updatedAt());
    }
}
