package co.edu.konradlorenz.kapp.schedule.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Wire shape of {@code Meeting}. Built only by {@code mapper.ScheduleMapper}.
 *
 * <p>{@code @JsonFormat(pattern = "HH:mm")} is required, not decorative: without it
 * Jackson's default {@code LocalTime} serializer writes seconds whenever they happen to be
 * zero-valued to begin with ({@code "18:15:00"}), which is a real value but not one the
 * contract's {@code LocalTime} schema - {@code ^([01][0-9]|2[0-3]):[0-5][0-9]$}, "No
 * seconds" - accepts.
 */
public record MeetingResponse(
        String meetingId,
        DayOfWeek dayOfWeek,

        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        List<MeetingPeriodResponse> periods
) {
}
