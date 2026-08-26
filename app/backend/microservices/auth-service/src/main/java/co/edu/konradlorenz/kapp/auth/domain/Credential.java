package co.edu.konradlorenz.kapp.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * What a person needs in order to sign in. Nothing else.
 *
 * <p>Profile data - name, programme, semester, avatar - lives in user-service. The two
 * used to be the same four tables mapped twice by two services, which is the blurred
 * boundary {@code docs/SECURITY-AUDIT.md} flagged. Keeping credentials alone in this
 * service is also what makes the planned Entra ID migration tractable: this collection
 * is the piece that disappears when the university becomes the identity provider, and
 * nothing else has to move.
 *
 * @param userId          shared identifier with user-service; becomes the {@code sub} claim
 * @param email           the login name, unique
 * @param passwordHash    BCrypt; null once an external identity provider owns this account
 * @param roles           prefixed role names, e.g. {@code ROLE_STUDENT}
 * @param status          {@link Status}
 * @param emailVerified   whether the address has been confirmed
 * @param provider        which identity provider owns this credential
 */
@Document(collection = "credentials")
public record Credential(
        @Id String id,
        String userId,
        String email,
        String passwordHash,
        List<String> roles,
        Status status,
        boolean emailVerified,
        Provider provider,
        Instant createdAt,
        Instant updatedAt
) {

    public enum Status {
        ACTIVE,
        /** Registered but the e-mail has not been confirmed yet. */
        PENDING_VERIFICATION,
        /** Disabled by an administrator. */
        SUSPENDED
    }

    /**
     * Which system vouches for this identity. {@code LOCAL} is the MVP; {@code ENTRA_ID}
     * is what the university's Microsoft tenant will provide once an application
     * registration is granted.
     */
    public enum Provider {
        LOCAL,
        ENTRA_ID
    }

    public boolean canSignIn() {
        return status == Status.ACTIVE;
    }
}
