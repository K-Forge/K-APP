package co.edu.konradlorenz.kapp.schedule.domain;

import java.time.LocalDate;

/**
 * One contiguous stretch of the semester during which a {@link Meeting} is taught in a
 * given room, both endpoints inclusive.
 *
 * <p>A weekly slot such as "Mondays 18:15-20:30" is not one continuous range: the source
 * SINU report prints several disjoint ranges per meeting, each with its own room, and some
 * of them carry no room at all. That is the one structural fact this whole domain model
 * exists to represent, so {@link Meeting#periods()} is a list of these rather than a
 * single pair of dates.
 *
 * @param from first date of the range, inclusive
 * @param to   last date of the range, inclusive, never earlier than {@code from}
 * @param room classroom code such as {@code "302"}, or {@code null} when no room has been
 *             assigned for this stretch - a real, common state in the source report
 */
public record MeetingPeriod(LocalDate from, LocalDate to, String room) {
}
