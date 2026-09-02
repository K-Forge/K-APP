package co.edu.konradlorenz.kapp.schedule.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The overlap rule as a table of cases, each independently named so a failure points at
 * exactly which rule broke.
 *
 * <p>Two meetings conflict iff they share a {@code dayOfWeek}, their time ranges intersect
 * as half-open {@code [start, end)}, and at least one pair of their date ranges intersects
 * as closed {@code [from, to]}. Every case below is checked in both argument orders, since
 * the rule is symmetric by definition and a bug that only shows up one way round would
 * otherwise slip through.
 */
class MeetingOverlapTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void evaluatesConflict(String description, Meeting a, Meeting b, boolean expectedConflict) {
        assertThat(MeetingOverlap.conflicts(a, b)).as(description).isEqualTo(expectedConflict);
        assertThat(MeetingOverlap.conflicts(b, a)).as(description + " (reversed)").isEqualTo(expectedConflict);
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(
                        "same day, overlapping times, overlapping dates: conflict",
                        meeting(DayOfWeek.MONDAY, "18:00", "20:00", period("2026-07-06", "2026-08-03")),
                        meeting(DayOfWeek.MONDAY, "19:00", "21:00", period("2026-07-20", "2026-08-17")),
                        true),

                Arguments.of(
                        "same day, same times, disjoint dates: no conflict",
                        meeting(DayOfWeek.MONDAY, "18:00", "20:00", period("2026-07-06", "2026-07-27")),
                        meeting(DayOfWeek.MONDAY, "18:00", "20:00", period("2026-09-07", "2026-09-28")),
                        false),

                Arguments.of(
                        "adjacent times touching at the boundary: no conflict "
                                + "(a class ending at 20:30 does not conflict with one starting at 20:30)",
                        meeting(DayOfWeek.MONDAY, "18:15", "20:30", period("2026-07-06", "2026-09-28")),
                        meeting(DayOfWeek.MONDAY, "20:30", "21:30", period("2026-07-06", "2026-09-28")),
                        false),

                Arguments.of(
                        "different days, identical time and date range: no conflict",
                        meeting(DayOfWeek.MONDAY, "18:00", "20:00", period("2026-07-06", "2026-09-28")),
                        meeting(DayOfWeek.TUESDAY, "18:00", "20:00", period("2026-07-06", "2026-09-28")),
                        false),

                Arguments.of(
                        "partial time overlap, same day, overlapping dates: conflict",
                        meeting(DayOfWeek.MONDAY, "18:00", "20:00", period("2026-07-06", "2026-09-28")),
                        meeting(DayOfWeek.MONDAY, "19:30", "21:00", period("2026-07-06", "2026-09-28")),
                        true),

                Arguments.of(
                        "date ranges touching at an inclusive boundary day: conflict",
                        meeting(DayOfWeek.MONDAY, "18:00", "19:00", period("2026-07-06", "2026-07-20")),
                        meeting(DayOfWeek.MONDAY, "18:00", "19:00", period("2026-07-20", "2026-08-03")),
                        true),

                Arguments.of(
                        "date ranges on consecutive Mondays with no shared day: no conflict",
                        meeting(DayOfWeek.MONDAY, "18:00", "19:00", period("2026-07-06", "2026-07-13")),
                        meeting(DayOfWeek.MONDAY, "18:00", "19:00", period("2026-07-20", "2026-07-27")),
                        false),

                Arguments.of(
                        "matching single-day periods: conflict",
                        meeting(DayOfWeek.MONDAY, "18:00", "19:00", period("2026-11-09", "2026-11-09")),
                        meeting(DayOfWeek.MONDAY, "18:30", "19:30", period("2026-11-09", "2026-11-09")),
                        true),

                Arguments.of(
                        "one meeting's later period overlaps the other's only period: conflict",
                        meeting(DayOfWeek.MONDAY, "18:00", "19:00",
                                period("2026-07-06", "2026-07-13"), period("2026-08-24", "2026-09-14")),
                        meeting(DayOfWeek.MONDAY, "18:00", "19:00", period("2026-09-07", "2026-09-28")),
                        true),

                Arguments.of(
                        "entirely unrelated day, time and dates: no conflict",
                        meeting(DayOfWeek.FRIDAY, "07:00", "09:00", period("2026-07-06", "2026-11-27")),
                        meeting(DayOfWeek.WEDNESDAY, "18:15", "21:15", period("2026-07-06", "2026-11-27")),
                        false)
        );
    }

    private static Meeting meeting(DayOfWeek dayOfWeek, String start, String end, MeetingPeriod... periods) {
        return new Meeting("meeting-id", dayOfWeek, LocalTime.parse(start), LocalTime.parse(end),
                List.of(periods));
    }

    private static MeetingPeriod period(String from, String to) {
        return new MeetingPeriod(LocalDate.parse(from), LocalDate.parse(to), "302");
    }
}
