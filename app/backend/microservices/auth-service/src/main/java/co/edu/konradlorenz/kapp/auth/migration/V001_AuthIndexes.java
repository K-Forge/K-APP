package co.edu.konradlorenz.kapp.auth.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Baseline indexes for credentials.
 *
 * <p>Change units are append-only. Never edit one that has run; add a new one.
 */
@ChangeUnit(id = "auth-indexes-v001", order = "001", author = "kapp")
public class V001_AuthIndexes {

    private static final String COLLECTION = "credentials";

    @Execution
    public void execute(MongoTemplate mongo) {
        // The unique constraint is what actually prevents two accounts on one address.
        // Checking existence before insert is a race; this is the guarantee.
        mongo.indexOps(COLLECTION).createIndex(
                new Index().on("email", Sort.Direction.ASC).unique().named("uk_credentials_email"));

        mongo.indexOps(COLLECTION).createIndex(
                new Index().on("userId", Sort.Direction.ASC).unique().named("uk_credentials_user"));

        // Verification tokens are looked up directly and should not linger; sparse so
        // only pending accounts occupy the index.
        mongo.indexOps(COLLECTION).createIndex(
                new Index().on("verificationToken", Sort.Direction.ASC)
                        .sparse().named("ix_credentials_verification"));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.indexOps(COLLECTION).dropIndex("uk_credentials_email");
        mongo.indexOps(COLLECTION).dropIndex("uk_credentials_user");
        mongo.indexOps(COLLECTION).dropIndex("ix_credentials_verification");
    }
}
