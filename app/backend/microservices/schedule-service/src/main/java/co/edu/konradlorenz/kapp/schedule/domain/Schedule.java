package co.edu.konradlorenz.kapp.schedule.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A student's whole timetable for one academic period, with every enrollment, meeting and
 * meeting period embedded.
 *
 * <p>One document per student per period, read and written whole: a timetable is always
 * fetched by its owner in a single call ({@code GET /api/schedule/me}), so embedding costs
 * one query where a normalised, referenced model would cost several. {@code userId} is the
 * {@code sub} claim of the owner's token - see {@link co.edu.konradlorenz.kapp.common.security.CurrentUser}
 * - which is an opaque id, not the numeric student code the SINU report prints.
 *
 * @param period the university's own {@code YYYYS} academic period code, e.g.
 *               {@code "20262"}. Validated with
 *               {@code co.edu.konradlorenz.kapp.common.academic.ValidAcademicPeriod} at the
 *               web boundary; stored as the plain wire string here.
 * @param active whether this is the student's current schedule. At most one schedule is
 *               active per student at a time - enforced in the service, not by a database
 *               constraint - and it is the one served when {@code period} is omitted.
 */
@Document(collection = "schedules")
public record Schedule(
        @Id
        String id,
        String userId,
        String period,
        String programCode,
        String pensumCode,
        Integer level,
        boolean active,
        List<Enrollment> enrollments
) {

    public Schedule {
        enrollments = enrollments == null ? List.of() : List.copyOf(enrollments);
    }

    public Schedule withActive(boolean newActive) {
        return newActive == active ? this
                : new Schedule(id, userId, period, programCode, pensumCode, level, newActive, enrollments);
    }

    public Schedule withEnrollments(List<Enrollment> newEnrollments) {
        return new Schedule(id, userId, period, programCode, pensumCode, level, active, newEnrollments);
    }

    public Optional<Enrollment> enrollment(String enrollmentId) {
        return enrollments.stream().filter(e -> e.enrollmentId().equals(enrollmentId)).findFirst();
    }

    /** @return a copy with {@code enrollment} appended */
    public Schedule addingEnrollment(Enrollment enrollment) {
        List<Enrollment> updated = new java.util.ArrayList<>(enrollments);
        updated.add(enrollment);
        return withEnrollments(updated);
    }

    /** @return a copy with the enrollment sharing {@code updated}'s id replaced */
    public Schedule replacingEnrollment(Enrollment updated) {
        return withEnrollments(enrollments.stream()
                .map(e -> e.enrollmentId().equals(updated.enrollmentId()) ? updated : e)
                .collect(Collectors.toList()));
    }

    /** @return a copy with the enrollment matching {@code enrollmentId} removed */
    public Schedule withoutEnrollment(String enrollmentId) {
        return withEnrollments(enrollments.stream()
                .filter(e -> !e.enrollmentId().equals(enrollmentId))
                .collect(Collectors.toList()));
    }
}
