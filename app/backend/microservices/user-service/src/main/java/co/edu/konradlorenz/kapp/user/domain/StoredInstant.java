package co.edu.konradlorenz.kapp.user.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Produces instants at the precision MongoDB can actually store.
 *
 * <p>BSON keeps a date as milliseconds since the epoch, while {@link Instant#now()} on a
 * modern JVM carries microseconds. An object built in memory and the same object read
 * back from Mongo therefore differ in their last digits and are never {@code equals},
 * which shows up as a baffling assertion failure in any test that seeds a document and
 * compares it to what it fetched.
 *
 * <p>Truncating here, at the one seam where timestamps enter the domain, is what keeps
 * the in-memory object and the stored one the same object.
 */
public final class StoredInstant {

    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** Truncates an instant supplied from outside, for seed data and tests. */
    public static Instant of(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.MILLIS);
    }

    private StoredInstant() {
    }
}
