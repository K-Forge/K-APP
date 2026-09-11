package co.edu.konradlorenz.kapp.schedule.web.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * A validated {@code PATCH .../meetings/{meetingId}} body. Built by
 * {@code web.MeetingPatchReader}.
 *
 * <p>When {@code periods} is present it replaces the whole list rather than merging into
 * it - the contract is explicit that a delta would make the disjoint-ranges list
 * ambiguous - so its {@link Patched} carries the complete new list, not one entry.
 */
public record UpdateMeetingRequest(
        Patched<DayOfWeek> dayOfWeek,
        Patched<LocalTime> startTime,
        Patched<LocalTime> endTime,
        Patched<List<MeetingPeriodRequest>> periods
) {
}
