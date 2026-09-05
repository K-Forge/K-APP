package co.edu.konradlorenz.kapp.auth.migration;

import co.edu.konradlorenz.kapp.auth.domain.VisitorPass;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import java.util.concurrent.TimeUnit;

/**
 * Indexes for the visitor day pass, including the one that enforces its retention period.
 *
 * <p>Change units are append-only. Never edit one that has run; add a new one.
 */
@ChangeUnit(id = "auth-visitor-passes-v003", order = "003", author = "kapp")
public class V003_VisitorPassIndexes {

    private static final String COLLECTION = VisitorPass.COLLECTION;

    @Execution
    public void execute(MongoTemplate mongo) {
        // A code is what a visitor types; it has to resolve in one hop and never collide.
        mongo.indexOps(COLLECTION).createIndex(
                new Index().on("code", Sort.Direction.ASC).unique().named("uk_visitor_pass_code"));

        // Reception's listing is "today's passes, newest first".
        mongo.indexOps(COLLECTION).createIndex(
                new Index().on("createdAt", Sort.Direction.DESC).named("ix_visitor_pass_created"));

        // THE RETENTION RULE, enforced by the database rather than by an application job.
        //
        // expireAfterSeconds(0) means "delete this document when the instant in purgeAt has
        // passed" - the field carries the deadline, so the 30-day window is set per document
        // when it is written rather than compiled into this index. MongoDB's TTL monitor
        // sweeps about once a minute, so deletion is prompt but not instantaneous.
        //
        // A scheduled task in this service could do the same thing and would be worse: it
        // stops running when the service is down, when somebody disables it, or when it
        // throws - and personal data quietly outstaying its retention period is exactly the
        // failure nobody notices until they are asked to prove it did not happen.
        mongo.getCollection(COLLECTION).createIndex(
                Indexes.ascending("purgeAt"),
                new IndexOptions().name("ttl_visitor_pass_purge").expireAfter(0L, TimeUnit.SECONDS));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.indexOps(COLLECTION).dropIndex("uk_visitor_pass_code");
        mongo.indexOps(COLLECTION).dropIndex("ix_visitor_pass_created");
        mongo.indexOps(COLLECTION).dropIndex("ttl_visitor_pass_purge");
    }
}
