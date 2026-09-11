package co.edu.konradlorenz.kapp.auth.migration;

import com.mongodb.client.model.UpdateOptions;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import java.time.Instant;
import java.util.Date;

/**
 * Invitation codes: the index that makes redemption safe, and the two seeded codes the
 * MVP demos with.
 *
 * <p>Change units are append-only. Never edit one that has run; add a new one.
 *
 * <h2>The seeded codes are public, and that is a decision with an expiry date</h2>
 * {@code KL-20262-STUDENT} and {@code KL-20262-STAFF} ship in this file so the MVP can be
 * demonstrated without an admin UI to mint codes. Anyone who can read this repository can
 * therefore create a student or professor account.
 *
 * <p>That is acceptable while the stack runs on a laptop behind Docker. It stops being
 * acceptable the moment the service is reachable from outside:
 * <strong>revoke both codes before any public deployment</strong> - set {@code active} to
 * false in a later change unit - and mint real per-intake codes instead. This is on the
 * pre-deployment checklist for exactly that reason.
 */
@ChangeUnit(id = "auth-invitation-codes-v002", order = "002", author = "kapp")
public class V002_InvitationCodes {

    private static final String COLLECTION = "invitation_codes";

    /** End of the 20262 academic period, with a margin. */
    private static final Instant SEED_EXPIRY = Instant.parse("2027-01-31T23:59:59Z");

    @Execution
    public void execute(MongoTemplate mongo) {
        // Redemption matches on `code`, and two codes with the same value would make
        // "which one did I just decrement" unanswerable.
        mongo.indexOps(COLLECTION).createIndex(
                new Index().on("code", Sort.Direction.ASC).unique().named("uk_invitation_code"));

        seed(mongo, "KL-20262-STUDENT", "ROLE_STUDENT", 200);
        seed(mongo, "KL-20262-STAFF", "ROLE_PROFESSOR", 20);
    }

    /**
     * {@code $setOnInsert} rather than {@code $set}: re-running this must not reset
     * {@code timesUsed} on a code that has already handed out accounts, which is what a
     * plain upsert would do if the change unit were ever replayed against a live database.
     */
    private void seed(MongoTemplate mongo, String code, String role, int maxUses) {
        Date now = Date.from(Instant.now());
        Document seed = new Document()
                .append("code", code)
                .append("role", role)
                .append("maxUses", maxUses)
                .append("timesUsed", 0)
                .append("active", true)
                .append("expiresAt", Date.from(SEED_EXPIRY))
                .append("createdAt", now)
                .append("updatedAt", now);

        mongo.getCollection(COLLECTION).updateOne(
                new Document("code", code),
                new Document("$setOnInsert", seed),
                new UpdateOptions().upsert(true));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.getCollection(COLLECTION).deleteMany(
                new Document("code", new Document("$in",
                        java.util.List.of("KL-20262-STUDENT", "KL-20262-STAFF"))));
        mongo.indexOps(COLLECTION).dropIndex("uk_invitation_code");
    }
}
