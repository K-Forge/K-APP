package co.edu.konradlorenz.kapp.schedule.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Baseline indexes for student timetables.
 *
 * <p>One document per student per academic period, with enrollments and their weekly
 * meetings embedded. A timetable is read whole, by its owner, so embedding costs one
 * query where a normalised model would cost several.
 *
 * <p>Change units are append-only. Never edit one that has run; add a new one.
 */
@ChangeUnit(id = "schedule-indexes-v001", order = "001", author = "kapp")
public class V001_ScheduleIndexes {

    private static final String COLLECTION = "schedules";

    @Execution
    public void execute(MongoTemplate mongo) {
        // A student has at most one timetable per period.
        mongo.indexOps(COLLECTION).createIndex(
                new Index().on("userId", Sort.Direction.ASC)
                        .on("period", Sort.Direction.ASC)
                        .unique().named("uk_schedule_user_period"));

        // Supports "give me my current timetable" without naming the period.
        mongo.indexOps(COLLECTION).createIndex(
                new Index().on("userId", Sort.Direction.ASC)
                        .on("active", Sort.Direction.DESC)
                        .named("ix_schedule_user_active"));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.indexOps(COLLECTION).dropIndex("uk_schedule_user_period");
        mongo.indexOps(COLLECTION).dropIndex("ix_schedule_user_active");
    }
}
