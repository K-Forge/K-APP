package co.edu.konradlorenz.kapp.semaphore.web.dto;

import co.edu.konradlorenz.kapp.common.academic.ValidAcademicPeriod;
import co.edu.konradlorenz.kapp.semaphore.domain.CourseStatus;
import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgressCourse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * New state for one item of the caller's semaforo.
 *
 * <p>The entry is replaced wholesale: omitting {@code grade} or {@code period} clears the
 * stored value rather than leaving it alone. That is the contract, and it is the reason
 * this is a PUT and not a PATCH.
 *
 * <p>The grade bounds are the university's own {@code 0..50} integer scale, mirroring the
 * {@code final_grade} constraint in the KApp database. Not 0..5, not 0..100. The
 * cross-field rules - a settled status needs a grade, a PENDING one must not carry a mark -
 * are enforced in the service, where a sibling field is visible.
 */
public record CourseProgressUpdateRequest(
        @NotNull CourseStatus status,
        @Min(StudentProgressCourse.MIN_GRADE) @Max(StudentProgressCourse.MAX_GRADE) Integer grade,
        @ValidAcademicPeriod String period
) {
}
