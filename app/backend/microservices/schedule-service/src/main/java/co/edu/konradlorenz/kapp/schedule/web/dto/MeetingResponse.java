package co.edu.konradlorenz.kapp.schedule.web.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/** Wire shape of {@code Meeting}. Built only by {@code mapper.ScheduleMapper}. */
public record MeetingResponse(
        String meetingId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        List<MeetingPeriodResponse> periods
) {
}
