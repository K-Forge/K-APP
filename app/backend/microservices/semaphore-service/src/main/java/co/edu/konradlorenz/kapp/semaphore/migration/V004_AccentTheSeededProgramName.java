package co.edu.konradlorenz.kapp.semaphore.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Puts the accents back on the seeded programme and faculty names.
 *
 * <p>The seed was written "Ingenieria de Sistemas" and "Facultad de Matematicas e Ingenierias" —
 * which is how it then appeared on every screen of the admin portal, and would have appeared in
 * the thesis demo. The university spells its own name with the accents, and this is the one piece
 * of institutional text the whole product shows by default.
 *
 * <p>{@code V002} is not edited: change units are append-only, and the databases that already ran
 * it would never see the correction. The seed files are fixed too, so a fresh database is right
 * from the start and this change unit finds nothing to do there.
 *
 * <p>Targeted by {@code _id} and by the exact unaccented string, so it cannot touch a name
 * somebody has since corrected by hand, and re-running it is a no-op.
 */
@ChangeUnit(id = "semaphore-accent-seeded-names-v004", order = "004", author = "kapp")
public class V004_AccentTheSeededProgramName {

    private static final Logger log = LoggerFactory.getLogger(V004_AccentTheSeededProgramName.class);

    private static final String PROGRAM_CODE = "506";
    private static final String PENSUM_CODE = "1015";

    private static final String PLAIN_NAME = "Ingenieria de Sistemas";
    private static final String ACCENTED_NAME = "Ingeniería de Sistemas";
    private static final String PLAIN_FACULTY = "Facultad de Matematicas e Ingenierias";
    private static final String ACCENTED_FACULTY = "Facultad de Matemáticas e Ingenierías";

    @Execution
    public void accent(MongoTemplate mongo) {
        long programs = mongo.updateFirst(
                new Query(Criteria.where("_id").is(PROGRAM_CODE)
                        .and("name").is(PLAIN_NAME)),
                new Update().set("name", ACCENTED_NAME).set("faculty", ACCENTED_FACULTY),
                "programs").getModifiedCount();

        long pensums = mongo.updateFirst(
                new Query(Criteria.where("_id").is(PENSUM_CODE)
                        .and("programName").is(PLAIN_NAME)),
                new Update().set("programName", ACCENTED_NAME).set("faculty", ACCENTED_FACULTY),
                "pensums").getModifiedCount();

        log.info("Accented {} program and {} pensum document(s)", programs, pensums);
    }

    @RollbackExecution
    public void unaccent(MongoTemplate mongo) {
        mongo.updateFirst(
                new Query(Criteria.where("_id").is(PROGRAM_CODE).and("name").is(ACCENTED_NAME)),
                new Update().set("name", PLAIN_NAME).set("faculty", PLAIN_FACULTY),
                "programs");
        mongo.updateFirst(
                new Query(Criteria.where("_id").is(PENSUM_CODE).and("programName").is(ACCENTED_NAME)),
                new Update().set("programName", PLAIN_NAME).set("faculty", PLAIN_FACULTY),
                "pensums");
    }
}
