package co.edu.konradlorenz.kapp.auth.service;

import co.edu.konradlorenz.kapp.auth.domain.DocumentType;
import co.edu.konradlorenz.kapp.auth.domain.VisitorPass;
import co.edu.konradlorenz.kapp.auth.domain.VisitorPassRepository;
import co.edu.konradlorenz.kapp.auth.jwt.JwtIssuer;
import co.edu.konradlorenz.kapp.auth.web.VisitorPassRedemptionRequest;
import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.ConflictException;
import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.common.security.KappRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Day passes for visitors: reception mints one, a visitor redeems it, and the token that
 * comes back opens the campus map and nothing else.
 *
 * <h2>Why not an account</h2>
 * Open guest registration accepted any e-mail address and left a real account behind
 * forever. A visitor is on campus for an afternoon. A pass is a token with a deadline and a
 * record of who used it, and there is nothing to clean up afterwards.
 *
 * <h2>Why the token is not special</h2>
 * The pass issues an ordinary {@code ROLE_GUEST} token. That role is already refused
 * everywhere but the map — {@code semaphore-service} denies it, {@code schedule-service}
 * denies it, {@code map-service} allows it — so "the pass only opens the map" is the
 * authorization matrix the services already enforce and the tests already assert, not a
 * second mechanism that could drift away from it.
 */
@Service
public class VisitorPassService {

    private static final Logger log = LoggerFactory.getLogger(VisitorPassService.class);

    /** How long the token lives once a visitor redeems the pass. */
    static final Duration ACCESS_TTL = Duration.ofHours(24);

    /**
     * How long the record of the visit survives. Reception's need is short-horizon — who was
     * in the building this month — and every extra day is more personal data exposed by a
     * breach for no additional benefit. See {@code SECURITY-AUDIT.md}, S12.
     */
    static final Duration RETENTION = Duration.ofDays(30);

    /** A pass is for today: it stops being redeemable 12 hours after it is minted. */
    static final Duration REDEEMABLE_FOR = Duration.ofHours(12);

    /**
     * No I, O, 0 or 1. The code is read aloud across a counter and typed by somebody who has
     * never seen it written down, and those four are the pairs that get misheard.
     */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int GROUP = 4;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VisitorPassRepository passes;
    private final MongoTemplate mongo;
    private final JwtIssuer jwt;

    public VisitorPassService(VisitorPassRepository passes, MongoTemplate mongo, JwtIssuer jwt) {
        this.passes = passes;
        this.mongo = mongo;
        this.jwt = jwt;
    }

    public VisitorPass issue(String issuedBy, String notes) {
        Instant now = Instant.now();
        return passes.save(new VisitorPass(
                UUID.randomUUID().toString(),
                nextCode(),
                issuedBy,
                notes,
                now,
                now.plus(REDEEMABLE_FOR),
                null, null, null, null, null,
                // An unredeemed pass is not personal data, but it should not accumulate
                // either. It disappears on the same schedule as a redeemed one.
                now.plus(RETENTION)));
    }

    public List<VisitorPass> list(Boolean redeemed) {
        if (redeemed == null) {
            return passes.findAllByOrderByCreatedAtDesc();
        }
        return redeemed
                ? passes.findByRedeemedAtIsNotNullOrderByRedeemedAtDesc()
                : passes.findByRedeemedAtIsNullOrderByCreatedAtDesc();
    }

    /**
     * Revokes an unredeemed pass.
     *
     * @throws ConflictException if it has already been redeemed. A redeemed pass is no longer
     *                          a pass, it is the record of a visit, and deleting it on
     *                          request would make the register something reception could
     *                          quietly edit. It leaves on its own after
     *                          {@link #RETENTION}
     */
    public void revoke(String code) {
        VisitorPass pass = passes.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor pass", code));

        if (pass.redeemed()) {
            throw new ConflictException(
                    "Pass %s has already been redeemed and is now a visit record".formatted(code),
                    List.of(new ApiError.FieldIssue("reason", "already-redeemed")));
        }
        passes.delete(pass);
        log.info("Revoked unredeemed visitor pass {}", code);
    }

    /**
     * Exchanges a pass for a 24-hour, map-only token, recording who redeemed it.
     *
     * <p>The redemption is a single {@code findAndModify} guarded on {@code redeemedAt}
     * still being null, so it is the server that decides who wins when the same code is
     * presented twice at once. Reading the pass, checking it, and then writing it back would
     * let two visitors redeem one code — rarely, under load, and invisibly, which is the
     * worst way to find out that a register is wrong.
     */
    public JwtIssuer.IssuedToken redeem(String code, VisitorPassRedemptionRequest request) {
        Instant now = Instant.now();

        VisitorPass pass = passes.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor pass", code));

        if (pass.expiredAt(now)) {
            throw new ConflictException(
                    "Pass %s expired at %s. Ask reception for a new one".formatted(
                            code, pass.redeemableUntil()),
                    List.of(new ApiError.FieldIssue("reason", "expired")));
        }

        VisitorPass redeemed = mongo.findAndModify(
                Query.query(Criteria.where("code").is(code).and("redeemedAt").is(null)),
                new Update()
                        .set("redeemedAt", now)
                        .set("documentType", request.documentType().name())
                        .set("documentNumber", request.documentNumber())
                        .set("visitorName", request.visitorName())
                        .set("accessExpiresAt", now.plus(ACCESS_TTL))
                        .set("purgeAt", now.plus(RETENTION)),
                FindAndModifyOptions.options().returnNew(true),
                VisitorPass.class);

        if (redeemed == null) {
            throw new ConflictException(
                    "Pass %s has already been redeemed".formatted(code),
                    List.of(new ApiError.FieldIssue("reason", "already-redeemed")));
        }

        // The subject is the pass, not a person. There is no account to point at, and a
        // subject that looked like a user id would eventually be treated as one.
        String subject = "visitor:" + redeemed.id();

        // The document number is deliberately absent from this log line. It is in the
        // register, which has a retention period; a log line has none.
        log.info("Visitor pass {} redeemed, access until {}", code, redeemed.accessExpiresAt());

        return jwt.issue(subject, null, List.of(KappRoles.GUEST), ACCESS_TTL);
    }

    /** Grouped as {@code KL-V-7K2M-QX9P}: four characters at a time is what people read back. */
    private String nextCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder("KL-V-");
            for (int i = 0; i < GROUP * 2; i++) {
                if (i == GROUP) {
                    code.append('-');
                }
                code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            String candidate = code.toString();
            if (!passes.existsByCode(candidate)) {
                return candidate;
            }
        }
        // 32^8 values against at most a few hundred live passes; ten collisions in a row is
        // not bad luck, it is a broken random source, and minting a duplicate would be worse.
        throw new IllegalStateException("Could not generate an unused visitor pass code");
    }
}
