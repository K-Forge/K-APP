package co.edu.konradlorenz.kapp.semaphore.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Baseline indexes for the academic catalogue and student progress.
 *
 * <p>This service owns the catalogue as well as the semaforo, because the curriculum IS
 * the semaforo: the grid a student sees is the pensum coloured by their own progress.
 * Splitting them would turn the most-used screen into a two-service read.
 *
 * <p>Change units are append-only. Never edit one that has run; add a new one.
 */
@ChangeUnit(id = "semaphore-indexes-v001", order = "001", author = "kapp")
public class V001_SemaphoreIndexes {

    @Execution
    public void execute(MongoTemplate mongo) {
        mongo.indexOps("programs").createIndex(
                new Index().on("code", Sort.Direction.ASC).unique().named("uk_programs_code"));

        mongo.indexOps("curricula").createIndex(
                new Index().on("pensumCode", Sort.Direction.ASC).unique().named("uk_curricula_pensum"));

        mongo.indexOps("curricula").createIndex(
                new Index().on("programCode", Sort.Direction.ASC).named("ix_curricula_program"));

        // A student follows exactly one version of one pensum. Without this, two
        // concurrent first reads would each lazily create a progress document.
        mongo.indexOps("studentProgress").createIndex(
                new Index().on("userId", Sort.Direction.ASC)
                        .on("pensumCode", Sort.Direction.ASC)
                        .unique().named("uk_progress_user_pensum"));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.indexOps("programs").dropIndex("uk_programs_code");
        mongo.indexOps("curricula").dropIndex("uk_curricula_pensum");
        mongo.indexOps("curricula").dropIndex("ix_curricula_program");
        mongo.indexOps("studentProgress").dropIndex("uk_progress_user_pensum");
    }
}
