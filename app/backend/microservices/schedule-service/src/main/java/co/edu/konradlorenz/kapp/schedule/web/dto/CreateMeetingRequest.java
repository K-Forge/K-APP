package co.edu.konradlorenz.kapp.schedule.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Body of {@code POST .../meetings}, and the shape of each entry of {@code meetings[]} in
 * {@code CreateEnrollmentRequest}.
 *
 * <p>Structural checks that need more than one field at a time - {@code endTime} after
 * {@code startTime}, every period falling on {@code dayOfWeek}, periods disjoint from each
 * other - are not expressible as annotations here and are done in
 * {@code service.MeetingWriteValidation} instead, alongside the overlap check against the
 * rest of the schedule.
 *
 * <p>{@code @JsonFormat(pattern = "HH:mm")} matches the contract's {@code LocalTime}
 * schema, which forbids seconds, and rejects a request that sends them rather than
 * silently truncating - see {@code MeetingResponse} for the read-side half of this.
 */
public record CreateMeetingRequest(

        @NotNull(message = "must not be null")
        DayOfWeek dayOfWeek,

        @NotNull(message = "must not be null")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @NotNull(message = "must not be null")
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        @NotEmpty(message = "must contain at least one date range")
        @Valid
        List<MeetingPeriodRequest> periods
) {
}
