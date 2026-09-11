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
 * <p>This service owns the catalogue as well as the semaforo, because the pensum IS
 * the semaforo: the grid a student sees is the pensum coloured by their own progress.
 * Splitting them would turn the most-used screen into a two-service read.
 *
 * <h2>{@code Program.code} and {@code Pensum.pensumCode} get no explicit index</h2>
 * Both are declared {@code @Id}, so MongoDB already backs each with its own implicit,
 * unique {@code _id} index. An explicit {@code unique()} index built by field name over
 * either one is resolved by Spring Data against the same {@code _id} key - because that
 * is what the field is mapped to - and the server rejects it outright:
 * {@code InvalidIndexSpecificationOption}, "the field 'unique' is not valid for an _id
 * index specification". There is nothing to add here; the uniqueness this migration
 * once tried to declare separately was already guaranteed.
 *
 * <p>Change units are append-only. Never edit one that has run; add a new one.
 */
@ChangeUnit(id = "semaphore-indexes-v001", order = "001", author = "kapp")
public class V001_SemaphoreIndexes {

    @Execution
    public void execute(MongoTemplate mongo) {
        mongo.indexOps("pensums").createIndex(
                new Index().on("programCode", Sort.Direction.ASC).named("ix_pensums_program"));

        // A student follows exactly one version of one pensum. Without this, two
        // concurrent first reads would each lazily create a progress document.
        mongo.indexOps("studentProgress").createIndex(
                new Index().on("userId", Sort.Direction.ASC)
                        .on("pensumCode", Sort.Direction.ASC)
                        .unique().named("uk_progress_user_pensum"));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.indexOps("pensums").dropIndex("ix_pensums_program");
        mongo.indexOps("studentProgress").dropIndex("uk_progress_user_pensum");
    }
}
