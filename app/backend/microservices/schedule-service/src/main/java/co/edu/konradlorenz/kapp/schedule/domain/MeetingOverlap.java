package co.edu.konradlorenz.kapp.schedule.domain;

import java.time.LocalTime;
import java.util.Optional;

/**
 * The one piece of real domain logic in this service: when do two weekly slots collide?
 *
 * <p>Two meetings conflict when all three hold:
 * <ol>
 *   <li>they share the same {@code dayOfWeek};</li>
 *   <li>their time ranges intersect, treated as half-open {@code [startTime, endTime)} -
 *       a class ending at {@code 20:30} and one starting at {@code 20:30} do not
 *       conflict;</li>
 *   <li>at least one {@link MeetingPeriod} of the first intersects at least one of the
 *       second, treated as closed {@code [from, to]} - both endpoints inclusive.</li>
 * </ol>
 *
 * <p>Two classes on the same weekday and hour whose date ranges never overlap are legal
 * and common in this university's calendar: a Monday evening slot that runs
 * July-September in room 302 does not conflict with an unrelated Monday evening slot that
 * only runs October-November.
 */
public final class MeetingOverlap {

    private MeetingOverlap() {
    }

    /** @return true when {@code a} and {@code b} conflict under the rule above */
    public static boolean conflicts(Meeting a, Meeting b) {
        return findConflict(a, b).isPresent();
    }

    /**
     * Same test as {@link #conflicts}, but also reports which pair of periods collided -
     * the detail an error message needs and a plain boolean cannot carry. Returns the
     * first colliding pair found; a meeting whose periods are themselves disjoint (as
     * required at write time) has at most one real answer per counterpart meeting for the
     * cases this service produces, but a caller only needs one example to explain a 409.
     */
    public static Optional<PeriodOverlap> findConflict(Meeting a, Meeting b) {
        if (a.dayOfWeek() != b.dayOfWeek()) {
            return Optional.empty();
        }
        if (!timesOverlap(a.startTime(), a.endTime(), b.startTime(), b.endTime())) {
            return Optional.empty();
        }
        for (MeetingPeriod p : a.periods()) {
            for (MeetingPeriod q : b.periods()) {
                if (periodsOverlap(p, q)) {
                    return Optional.of(new PeriodOverlap(p, q));
                }
            }
        }
        return Optional.empty();
    }

    /** Half-open {@code [start, end)}: touching at the boundary is not an overlap. */
    private static boolean timesOverlap(LocalTime startA, LocalTime endA,
                                        LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    /**
     * Closed {@code [from, to]}: both endpoints inclusive, so two ranges that share a
     * single boundary day (e.g. one ending and the next starting the same Monday) do
     * intersect. Used both for two different meetings' periods here, and for validating
     * that one meeting's own periods stay disjoint from each other - see
     * {@code MeetingWriteValidation} in the {@code service} package.
     */
    public static boolean periodsOverlap(MeetingPeriod p, MeetingPeriod q) {
        return !p.from().isAfter(q.to()) && !q.from().isAfter(p.to());
    }

    /** The pair of periods whose date ranges intersected, for building a 409 message. */
    public record PeriodOverlap(MeetingPeriod first, MeetingPeriod second) {
    }
}
