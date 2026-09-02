package co.edu.konradlorenz.kapp.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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
 * <h2>{@code emailVerified} and {@code status} are different facts</h2>
 * {@code emailVerified} records whether the address was ever actually proven. It is
 * {@code false} for every freshly registered account, without exception, because at that
 * moment nothing has been proven. {@code status} records whether the account may sign in,
 * and that is a policy decision: {@link Status#ACTIVE} when the deployment does not
 * require verification, {@link Status#PENDING_VERIFICATION} when it does.
 *
 * <p>Sign-in gates on {@code status}, never on {@code emailVerified}. Collapsing the two
 * - creating accounts as verified so they can log in while there is no SMTP relay - makes
 * an account claim a verification that never happened, and once the flag is switched on
 * those accounts are indistinguishable from genuinely verified ones.
 *
 * @param userId                     shared identifier with user-service; becomes the {@code sub} claim
 * @param email                      the login name, unique, stored lower-cased
 * @param passwordHash               BCrypt; null once an external identity provider owns this account
 * @param roles                      prefixed role names, e.g. {@code ROLE_STUDENT}
 * @param status                     {@link Status}
 * @param emailVerified              whether the address has been confirmed
 * @param provider                   which identity provider owns this credential
 * @param verificationTokenHash      SHA-256 of the outstanding verification token, never the
 *                                   token itself; null once consumed. Stored under the field
 *                                   name {@code verificationToken}, which
 *                                   {@code V001_AuthIndexes} already indexes.
 * @param verificationTokenExpiresAt when that token stops being accepted
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
        @Field("verificationToken") String verificationTokenHash,
        Instant verificationTokenExpiresAt,
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

    /**
     * A brand new local account.
     *
     * <p>{@code emailVerified} is hard-coded to {@code false} here rather than taken as a
     * parameter: there is no path through registration that legitimately produces a
     * verified address, so the type refuses to express one.
     */
    public static Credential newLocalAccount(String userId, String email, String passwordHash,
                                             List<String> roles, Status status, Instant now) {
        return new Credential(null, userId, email, passwordHash, roles, status, false,
                Provider.LOCAL, null, null, now, now);
    }
}
