package co.edu.konradlorenz.kapp.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A code the registrar mints for an intake, which decides the role an account receives.
 *
 * <p>{@code timesUsed} is never read and then written back. Redemption is a single
 * {@code findAndModify} guarded by
 * {@code $expr: {$lt: ["$timesUsed", "$maxUses"]}}, so the check and the increment are one
 * atomic operation on the server: two simultaneous registrations against the last
 * remaining use of a code cannot both win. Read-then-write loses that race silently and
 * only under load, which is the worst way to find out.
 *
 * @param code      the value a registrant types, e.g. {@code KL-20262-STUDENT}
 * @param role      the prefixed role this code grants, e.g. {@code ROLE_STUDENT}
 * @param maxUses   how many accounts this code may create in total
 * @param timesUsed how many it has created so far
 * @param active    an off switch the registrar can flip without deleting history
 * @param expiresAt when the code stops working; null means it never expires on its own
 */
@Document(collection = "invitation_codes")
public record InvitationCode(
        @Id String id,
        String code,
        String role,
        int maxUses,
        int timesUsed,
        boolean active,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static final String COLLECTION = "invitation_codes";

    public int remainingUses() {
        return Math.max(0, maxUses - timesUsed);
    }
}
