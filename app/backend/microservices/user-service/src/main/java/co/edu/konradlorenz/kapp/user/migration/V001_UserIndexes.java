package co.edu.konradlorenz.kapp.user.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;

/**
 * Baseline indexes for the {@code users} collection.
 *
 * <p>Mongock gives this project what it never had with PostgreSQL: versioned, ordered,
 * audited schema changes with a distributed lock, so four developers and CI can point at
 * the same MongoDB without racing each other. The previous setup applied a 552-line
 * {@code init.sql} by hand, with no migration tool at all.
 *
 * <p>Change units are append-only. Never edit one that has run; add a new one.
 */
@ChangeUnit(id = "user-indexes-v001", order = "001", author = "kapp")
public class V001_UserIndexes {

    private static final String COLLECTION = "users";

    @Execution
    public void execute(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(COLLECTION)
                .createIndex(new Index().on("email", Sort.Direction.ASC).unique().named("uk_users_email"));

        mongoTemplate.indexOps(COLLECTION)
                .createIndex(new Index().on("academic.studentCode", Sort.Direction.ASC)
                        .sparse().named("ix_users_student_code"));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(COLLECTION).dropIndex("uk_users_email");
        mongoTemplate.indexOps(COLLECTION).dropIndex("ix_users_student_code");
    }
}
