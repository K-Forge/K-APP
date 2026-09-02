package co.edu.konradlorenz.kapp.schedule.web.dto;

import java.time.LocalTime;

/**
 * One class actually taking place on one concrete date, already resolved server-side:
 * wire shape of {@code ClassOccurrence}. Built only by
 * {@code mapper.ScheduleMapper#toClassOccurrence}, which is the single place
 * {@link co.edu.konradlorenz.kapp.schedule.domain.MeetingResolution} results are turned
 * into this shape - used identically by both the day and the week endpoint.
 */
public record ClassOccurrenceResponse(
        String enrollmentId,
        String courseCode,
        String courseName,
        String professor,
        LocalTime startTime,
        LocalTime endTime,
        String room,
        String campus,
        String color
) {
}
