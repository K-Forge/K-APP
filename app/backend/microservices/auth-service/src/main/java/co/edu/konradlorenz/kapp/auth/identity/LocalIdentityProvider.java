package co.edu.konradlorenz.kapp.auth.identity;

import co.edu.konradlorenz.kapp.auth.domain.Credential;
import co.edu.konradlorenz.kapp.auth.domain.CredentialRepository;
import co.edu.konradlorenz.kapp.common.error.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Verifies a password against the BCrypt hash this service stores itself.
 *
 * <p>The only implementation of {@link IdentityProviderPort} in the MVP, and the only
 * class in the service that knows a password hash exists. An Entra adapter will sit
 * beside it, supporting {@link IdentityAssertion.AuthorizationCode} instead.
 *
 * <h2>Failure modes are deliberately uneven, and deliberately so in one direction only</h2>
 * Unknown account, wrong password and suspended account all raise the same
 * {@link InvalidCredentialsException} with the same message, so the endpoint cannot be
 * used to enumerate which university addresses have accounts.
 *
 * <p>An unverified address is the documented exception: the contract requires HTTP 403
 * there so clients can route the user to the "resend verification" screen. That is a
 * narrow account oracle, accepted knowingly - it only speaks to someone who already knows
 * the password, which is the harder half.
 */
@Component
public class LocalIdentityProvider implements IdentityProviderPort {

    private static final Logger log = LoggerFactory.getLogger(LocalIdentityProvider.class);

    public static final String PROVIDER_ID = "local";

    /**
     * A real BCrypt hash of a random value, verified against when no account matches.
     * Without this, a request for an unknown e-mail returns noticeably faster than one
     * for a known e-mail with a wrong password, and that timing difference is itself an
     * account oracle.
     */
    private static final String DUMMY_HASH =
            "$2a$12$C6UzMDM.H6dfI/f/IKcEe.3PjPYFCkPFHU8b0aVEUOsHZQGkGpuHy";

    private final CredentialRepository credentials;
    private final PasswordEncoder passwordEncoder;

    public LocalIdentityProvider(CredentialRepository credentials, PasswordEncoder passwordEncoder) {
        this.credentials = credentials;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean supports(IdentityAssertion assertion) {
        return assertion instanceof IdentityAssertion.Password;
    }

    @Override
    public AuthenticatedIdentity authenticate(IdentityAssertion assertion) {
        if (!(assertion instanceof IdentityAssertion.Password password)) {
            throw new IllegalArgumentException(
                    "LocalIdentityProvider only handles password assertions");
        }

        String email = normalise(password.email());
        Optional<Credential> found = credentials.findByEmailIgnoreCase(email);

        // Always run a BCrypt comparison, even when nothing matched, so the response time
        // does not reveal whether the address is known.
        String hash = found.map(Credential::passwordHash).orElse(DUMMY_HASH);
        boolean passwordMatches =
                passwordEncoder.matches(password.rawPassword(), hash == null ? DUMMY_HASH : hash);

        if (found.isEmpty() || !passwordMatches) {
            // Logged with the reason, returned without it.
            log.info("Failed sign-in for {}: known={}, passwordMatches={}",
                    email, found.isPresent(), passwordMatches);
            throw new InvalidCredentialsException();
        }

        Credential credential = found.get();

        // Gates on status, not on emailVerified. The two answer different questions and
        // an account can legitimately be ACTIVE with an unproven address when the
        // deployment does not require verification.
        if (credential.status() == Credential.Status.PENDING_VERIFICATION) {
            log.info("Sign-in refused for {}: address not confirmed yet", email);
            throw new AccessDeniedException("E-mail address has not been confirmed");
        }
        if (!credential.canSignIn()) {
            log.info("Sign-in refused for {}: status={}", email, credential.status());
            throw new InvalidCredentialsException();
        }

        return new AuthenticatedIdentity(
                credential.userId(), credential.email(), credential.roles(),
                credential.emailVerified());
    }

    private static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
