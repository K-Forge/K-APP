package co.edu.konradlorenz.kapp.schedule.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * One weekly day + time slot of a course, together with the disjoint date ranges over
 * which it is actually taught.
 *
 * <p>{@code startTime}/{@code endTime} are compared as a half-open range
 * {@code [startTime, endTime)} for overlap purposes: a class ending at {@code 20:30} does
 * not conflict with one starting at {@code 20:30}. See {@link MeetingOverlap}.
 *
 * @param meetingId server-assigned identifier
 * @param dayOfWeek the weekday this slot repeats on. {@link DayOfWeek}'s own constants
 *                  (MONDAY..SUNDAY) match the contract's {@code DayOfWeek} enum exactly,
 *                  so no translation is needed at the boundary
 * @param startTime when the class starts, inclusive
 * @param endTime   when the class ends, exclusive for overlap purposes, always after
 *                  {@code startTime}
 * @param periods   the disjoint date ranges this slot is taught over, ascending by
 *                  {@code from}. Never empty.
 */
public record Meeting(
        String meetingId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        List<MeetingPeriod> periods
) {

    public Meeting {
        periods = periods == null ? List.of() : List.copyOf(periods);
    }
}
