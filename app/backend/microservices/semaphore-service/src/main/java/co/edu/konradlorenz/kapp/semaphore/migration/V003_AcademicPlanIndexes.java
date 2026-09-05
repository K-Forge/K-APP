package co.edu.konradlorenz.kapp.semaphore.migration;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Indexes for the student's own academic plans.
 *
 * <p>Change units are append-only. Never edit one that has run; add a new one.
 */
@ChangeUnit(id = "semaphore-academic-plans-v003", order = "003", author = "kapp")
public class V003_AcademicPlanIndexes {

    private static final String COLLECTION = "academicPlans";

    @Execution
    public void execute(MongoTemplate mongo) {
        // Every listing is "this student's plans", ordered.
        mongo.indexOps(COLLECTION).createIndex(
                new Index().on("userId", Sort.Direction.ASC)
                        .on("pensumCode", Sort.Direction.ASC)
                        .named("ix_plans_user_pensum"));

        // Exactly one primary per student and pensum, enforced by the server rather than by
        // the service remembering to demote the previous one. A PARTIAL index is required:
        // a plain unique index over (userId, pensumCode, primary) would also forbid a second
        // NON-primary plan, and holding several alternatives is the entire point of the
        // feature. The filter restricts uniqueness to the documents where primary is true.
        mongo.getCollection(COLLECTION).createIndex(
                Indexes.ascending("userId", "pensumCode"),
                new IndexOptions()
                        .unique(true)
                        .name("uk_plans_one_primary")
                        .partialFilterExpression(new Document("primary", true)));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.indexOps(COLLECTION).dropIndex("ix_plans_user_pensum");
        mongo.indexOps(COLLECTION).dropIndex("uk_plans_one_primary");
    }
}
