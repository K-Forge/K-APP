package co.edu.konradlorenz.kapp.schedule.web.dto;

import java.time.LocalDate;

/**
 * A validated {@code PATCH /api/schedule/me/enrollments/{enrollmentId}} body. Built by
 * {@code web.EnrollmentPatchReader}, the only place that reads the raw JSON tree - see
 * {@code user-service}'s {@code ProfilePatchReader} for why a record cannot bind this
 * directly.
 *
 * <p>Deliberately has no {@code meetings} field: the contract rejects that key with 400,
 * and weekly slots are only ever changed through the {@code /meetings} sub-resource.
 */
public record UpdateEnrollmentRequest(
        Patched<String> courseCode,
        Patched<String> pensumItemCode,
        Patched<String> courseName,
        Patched<Integer> level,
        Patched<Integer> credits,
        Patched<Integer> totalHours,
        Patched<String> group,
        Patched<String> subgroup,
        Patched<String> professor,
        Patched<String> campus,
        Patched<LocalDate> startDate,
        Patched<LocalDate> endDate,
        Patched<String> color
) {
}
