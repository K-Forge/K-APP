package co.edu.konradlorenz.kapp.schedule.web.dto;

import java.util.List;

/**
 * Wire shape of {@code Schedule}. Built only by {@code mapper.ScheduleMapper}.
 *
 * <p>{@code userId} is the {@code sub} claim of the owner's token: an opaque string id,
 * not the {@code integer/int64} the contract's schema declares. See the top-level report
 * for why - in short, no id in this platform is ever numeric, and declaring one that way
 * here would be a contract bug this service should not paper over by lying to the caller.
 */
public record ScheduleResponse(
        String id,
        String userId,
        String period,
        String programCode,
        String pensumCode,
        Integer level,
        boolean active,
        List<EnrollmentResponse> enrollments
) {
}
