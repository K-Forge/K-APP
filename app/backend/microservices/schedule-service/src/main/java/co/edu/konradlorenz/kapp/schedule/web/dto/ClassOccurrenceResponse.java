package co.edu.konradlorenz.kapp.schedule.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

/**
 * One class actually taking place on one concrete date, already resolved server-side:
 * wire shape of {@code ClassOccurrence}. Built only by
 * {@code mapper.ScheduleMapper#toClassOccurrence}, which is the single place
 * {@link co.edu.konradlorenz.kapp.schedule.domain.MeetingResolution} results are turned
 * into this shape - used identically by both the day and the week endpoint.
 *
 * <p>{@code @JsonFormat(pattern = "HH:mm")} keeps the times on the contract's own
 * {@code LocalTime} format - see {@code MeetingResponse} for why this is not decorative.
 */
public record ClassOccurrenceResponse(
        String enrollmentId,
        String courseCode,
        String courseName,
        String professor,

        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        String room,
        String campus,
        String color
) {
}
