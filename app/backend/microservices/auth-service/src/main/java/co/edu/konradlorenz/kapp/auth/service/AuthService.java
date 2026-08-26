package co.edu.konradlorenz.kapp.auth.service;

import co.edu.konradlorenz.kapp.auth.domain.Credential;
import co.edu.konradlorenz.kapp.auth.domain.CredentialRepository;
import co.edu.konradlorenz.kapp.auth.jwt.JwtIssuer;
import co.edu.konradlorenz.kapp.common.error.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Verifies credentials and issues access tokens.
 *
 * <p>Everything here is deliberately uniform in its failure mode: unknown account, wrong
 * password and suspended account all raise the same exception with the same message.
 * Distinguishing them would let anyone enumerate which university e-mails have accounts.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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
    private final JwtIssuer jwtIssuer;

    public AuthService(CredentialRepository credentials,
                       PasswordEncoder passwordEncoder,
                       JwtIssuer jwtIssuer) {
        this.credentials = credentials;
        this.passwordEncoder = passwordEncoder;
        this.jwtIssuer = jwtIssuer;
    }

    public JwtIssuer.IssuedToken login(String email, String rawPassword) {
        Optional<Credential> found = credentials.findByEmailIgnoreCase(normalise(email));

        String hash = found.map(Credential::passwordHash).orElse(DUMMY_HASH);
        boolean passwordMatches = passwordEncoder.matches(rawPassword,
                hash == null ? DUMMY_HASH : hash);

        Credential credential = found
                .filter(c -> passwordMatches)
                .filter(Credential::canSignIn)
                .orElseThrow(() -> {
                    // Logged with the reason, returned without it.
                    log.info("Failed sign-in for {}: known={}, passwordMatches={}",
                            normalise(email), found.isPresent(), passwordMatches);
                    return new InvalidCredentialsException();
                });

        log.info("Issued token for user {}", credential.userId());
        return jwtIssuer.issue(credential.userId(), credential.email(), credential.roles());
    }

    private static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
