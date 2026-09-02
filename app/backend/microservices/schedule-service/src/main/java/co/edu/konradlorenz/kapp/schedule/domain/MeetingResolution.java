package co.edu.konradlorenz.kapp.schedule.domain;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Resolves whether one {@link Meeting} actually occurs on one concrete date, and if so
 * which {@link MeetingPeriod} - and therefore which room - is in force.
 *
 * <p>This is the single helper behind both {@code GET /api/schedule/me/day} and
 * {@code GET /api/schedule/me/week}: the week view is nothing more than this rule applied
 * to each of the seven dates of the week, so the resolution logic exists exactly once.
 */
public final class MeetingResolution {

    private MeetingResolution() {
    }

    /**
     * @return the period covering {@code date}, if the meeting's {@code dayOfWeek}
     * matches {@code date}'s weekday and one of its periods contains it (both endpoints
     * inclusive). Empty when the class simply does not meet that day, or {@code date}
     * falls in a gap between two periods.
     */
    public static Optional<MeetingPeriod> periodOn(Meeting meeting, LocalDate date) {
        if (meeting.dayOfWeek() != date.getDayOfWeek()) {
            return Optional.empty();
        }
        return meeting.periods().stream()
                .filter(p -> !date.isBefore(p.from()) && !date.isAfter(p.to()))
                .findFirst();
    }
}
