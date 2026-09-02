package co.edu.konradlorenz.kapp.auth.service;

import co.edu.konradlorenz.kapp.auth.config.RegistrationProperties;
import co.edu.konradlorenz.kapp.auth.domain.Credential;
import co.edu.konradlorenz.kapp.auth.domain.CredentialRepository;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Mints, delivers and consumes e-mail verification tokens.
 *
 * <h2>The database never holds a usable token</h2>
 * A token is 32 bytes from {@link SecureRandom}, URL-safe base64. What is stored is its
 * SHA-256 hash. A dump of the {@code credentials} collection therefore contains nothing
 * replayable: an attacker holding it cannot confirm anyone's address. No salt and no
 * BCrypt here on purpose - the input is already 256 bits of entropy, so there is nothing
 * to brute-force and a slow hash would only make every verification slow.
 *
 * <h2>Single use, enforced by the database</h2>
 * Consuming a token is one {@code findAndModify} that matches on the hash and clears it in
 * the same operation. Two simultaneous clicks on the same link mean one 204 and one 400,
 * never two.
 *
 * <h2>Confirming is not the same as activating</h2>
 * Verification always sets {@code emailVerified = true}. It promotes
 * {@code PENDING_VERIFICATION} to {@code ACTIVE} as well, but only from that status: a
 * {@code SUSPENDED} account that follows an old link stays suspended.
 */
@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private static final String INVALID_TOKEN_MESSAGE =
            "The verification link is invalid or has expired. Request a new one.";

    /**
     * Identical wording for every outcome. A per-address distinction here would turn this
     * endpoint into an account enumeration oracle, which is exactly what the contract
     * forbids.
     */
    public static final String NEUTRAL_RESEND_MESSAGE =
            "If an account exists for that address, a verification e-mail has been sent.";

    private final MongoTemplate mongo;
    private final CredentialRepository credentials;
    private final VerificationMailer mailer;
    private final RegistrationProperties properties;

    public VerificationService(MongoTemplate mongo,
                               CredentialRepository credentials,
                               VerificationMailer mailer,
                               RegistrationProperties properties) {
        this.mongo = mongo;
        this.credentials = credentials;
        this.mailer = mailer;
        this.properties = properties;
    }

    /**
     * Issues a fresh token for an account and e-mails it.
     *
     * <p>Writing the new hash overwrites any previous one, which is what invalidates a
     * token that was issued earlier: exactly one verification link works at a time.
     */
    public void issueAndSend(Credential credential) {
        String rawToken = newToken();
        Instant expiry = Instant.now().plus(properties.verificationTtl());

        mongo.updateFirst(
                new Query(Criteria.where("id").is(credential.id())),
                new Update()
                        .set("verificationTokenHash", hash(rawToken))
                        .set("verificationTokenExpiresAt", expiry)
                        .set("updatedAt", Instant.now()),
                Credential.class);

        mailer.sendVerificationLink(credential.email(), rawToken);
    }

    /**
     * Consumes a token and confirms the address.
     *
     * @throws BusinessRuleException with HTTP 400 when the token is unknown, already used
     *         or expired. All three are one message: a client cannot act differently on
     *         them anyway, and it must simply ask for a new link.
     */
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessRuleException(INVALID_TOKEN_MESSAGE);
        }

        Instant now = Instant.now();

        // The single-use guarantee. Matching the hash and clearing it are one operation,
        // so a replay - or a second concurrent click - finds nothing to match.
        Credential claimed = mongo.findAndModify(
                new Query(Criteria.where("verificationTokenHash").is(hash(rawToken))
                        .and("verificationTokenExpiresAt").gt(now)),
                new Update()
                        .set("emailVerified", true)
                        .set("updatedAt", now)
                        .unset("verificationTokenHash")
                        .unset("verificationTokenExpiresAt"),
                FindAndModifyOptions.options().returnNew(false),
                Credential.class);

        if (claimed == null) {
            log.info("Verification refused: unknown, spent or expired token");
            throw new BusinessRuleException(INVALID_TOKEN_MESSAGE);
        }

        // Confirming an address activates an account that was waiting on it, and only
        // that: a suspended account is not resurrected by an old link.
        if (claimed.status() == Credential.Status.PENDING_VERIFICATION) {
            mongo.updateFirst(
                    new Query(Criteria.where("id").is(claimed.id())
                            .and("status").is(Credential.Status.PENDING_VERIFICATION)),
                    new Update()
                            .set("status", Credential.Status.ACTIVE)
                            .set("updatedAt", now),
                    Credential.class);
        }

        log.info("Address confirmed for user {}", claimed.userId());
    }

    /**
     * Issues a new token for an address, if there is anything to issue one for.
     *
     * <p>Says nothing either way. The caller answers 202 unconditionally.
     */
    public void resend(String email) {
        Optional<Credential> found = credentials.findByEmailIgnoreCase(normalise(email));

        if (found.isEmpty()) {
            log.info("Verification resend requested for an address with no account");
            return;
        }
        Credential credential = found.get();
        if (credential.emailVerified()) {
            log.info("Verification resend requested for an already confirmed address");
            return;
        }
        issueAndSend(credential);
    }

    /** 32 bytes of entropy, URL-safe so it survives being pasted into a link. */
    static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256, hex encoded. The only form of a token this service ever stores. */
    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
