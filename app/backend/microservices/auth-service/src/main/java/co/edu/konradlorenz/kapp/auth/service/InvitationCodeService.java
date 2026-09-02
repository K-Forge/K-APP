package co.edu.konradlorenz.kapp.auth.service;

import co.edu.konradlorenz.kapp.auth.domain.InvitationCode;
import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Claims and returns uses of an invitation code.
 *
 * <p><strong>Never read-then-write.</strong> A registration that loads a code, checks
 * {@code timesUsed < maxUses} in Java and saves the increment back will hand out the last
 * use of a code twice whenever two requests interleave, and it will do so silently and
 * only under load. Both operations here are a single guarded {@code findAndModify}, so the
 * check and the change happen together on the server.
 */
@Service
public class InvitationCodeService {

    private static final Logger log = LoggerFactory.getLogger(InvitationCodeService.class);

    private final MongoTemplate mongo;

    public InvitationCodeService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /**
     * Atomically claims one use of a code.
     *
     * @return the code as it stands after the claim
     * @throws BusinessRuleException when the code is unknown, inactive, expired or spent.
     *         All four are one message on purpose: telling a registrant that a code exists
     *         but is spent is more than they need to know.
     */
    public InvitationCode redeem(String rawCode) {
        String code = normalise(rawCode);
        Date now = Date.from(Instant.now());

        // Raw filter rather than Criteria: $expr compares two fields of the same document,
        // which is the whole point - the cap travels with the code instead of being read
        // into the application first.
        Document filter = new Document("code", code)
                .append("active", true)
                .append("$or", List.of(
                        new Document("expiresAt", null),
                        new Document("expiresAt", new Document("$gt", now))))
                .append("$expr", new Document("$lt", List.of("$timesUsed", "$maxUses")));

        Update update = new Update().inc("timesUsed", 1).set("updatedAt", now);

        InvitationCode redeemed = mongo.findAndModify(
                new BasicQuery(filter), update,
                FindAndModifyOptions.options().returnNew(true),
                InvitationCode.class, InvitationCode.COLLECTION);

        if (redeemed == null) {
            log.info("Invitation code refused: {}", code);
            throw new BusinessRuleException("Invitation code is invalid, expired or already used",
                    List.of(new ApiError.FieldIssue("invitationCode",
                            "Unknown, expired or already used")));
        }

        log.info("Invitation code {} redeemed, {} of {} uses left",
                code, redeemed.remainingUses(), redeemed.maxUses());
        return redeemed;
    }

    /**
     * Returns a use claimed by a registration that then failed.
     *
     * <p>Guarded by {@code $expr: {$gt: ["$timesUsed", 0]}} so a double release - a retry,
     * a compensating path that runs twice - cannot drive the counter below zero and hand
     * out more accounts than the registrar authorised.
     */
    public void release(String rawCode) {
        String code = normalise(rawCode);
        Document filter = new Document("code", code)
                .append("$expr", new Document("$gt", List.of("$timesUsed", 0)));

        Update update = new Update().inc("timesUsed", -1).set("updatedAt", Date.from(Instant.now()));

        InvitationCode released = mongo.findAndModify(
                new BasicQuery(filter), update,
                FindAndModifyOptions.options().returnNew(true),
                InvitationCode.class, InvitationCode.COLLECTION);

        if (released == null) {
            log.warn("Could not release invitation code {}: unknown or already at zero", code);
        } else {
            log.info("Released invitation code {}, {} uses left", code, released.remainingUses());
        }
    }

    /** Read-only lookup, for diagnostics and tests. Never the basis of a redemption. */
    public InvitationCode find(String rawCode) {
        Query query = new BasicQuery(new Document("code", normalise(rawCode)));
        return mongo.findOne(query, InvitationCode.class, InvitationCode.COLLECTION);
    }

    private static String normalise(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
