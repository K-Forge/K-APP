package co.edu.konradlorenz.kapp.map.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * *** SUPERSEDED BY {@code V003_SchematicCampusSeed}. DELIBERATELY DOES NOTHING. ***
 *
 * <p>This change unit used to seed a placeholder campus in the old map model: floors carrying
 * a plan image and its pixel dimensions, spaces pinned at a percentage of that image. The map
 * is now described as a grid the client draws, and those documents cannot be expressed in the
 * new model at all.
 *
 * <p>It is emptied rather than rewritten, and rather than deleted, because change units are
 * append-only: Mongock has already recorded this id as executed on every existing volume and
 * will never run it again there. Rewriting its body would therefore have seeded nothing on
 * exactly the machines that need the new data, while looking like it had. Deleting the class
 * would leave that record pointing at nothing.
 *
 * <p>{@code V003_SchematicCampusSeed} does the work: it removes whatever this one wrote -
 * scoped to {@code placeholder = true} - and seeds the schematic campus in its place. That
 * makes an existing volume converge and a fresh one arrive at the same state.
 */
@ChangeUnit(id = "map-placeholder-campus-seed-v002", order = "002", author = "kapp")
public class V002_PlaceholderCampusSeed {

    @Execution
    public void execute(MongoTemplate mongo) {
        // Intentionally empty. See the class javadoc.
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        // Nothing to undo.
    }
}
