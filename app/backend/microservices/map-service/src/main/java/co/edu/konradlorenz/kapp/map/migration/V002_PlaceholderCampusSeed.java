package co.edu.konradlorenz.kapp.map.migration;

import co.edu.konradlorenz.kapp.map.domain.BuildingDocument;
import co.edu.konradlorenz.kapp.map.domain.Floor;
import co.edu.konradlorenz.kapp.map.domain.SpaceDocument;
import co.edu.konradlorenz.kapp.map.domain.SpaceType;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * *** PLACEHOLDER DATA - NOT THE REAL CAMPUS ***
 *
 * <p>Real floor plan photography and a survey of every room do not exist yet, and obtaining
 * them is human latency (someone has to walk the campus with a camera and a tape measure).
 * The mobile team cannot wait on that to start building against a populated map, so this
 * change unit invents a small, plausible campus: one Sede, two buildings, and the handful of
 * room codes {@code schedule-service} already needs for its own tests to resolve.
 *
 * <p>Every document this writes carries {@code placeholder = true}. That flag is
 * storage-only - {@code MapMapper} never puts it on a response - so a client cannot
 * accidentally brand it "seed data" in the UI, but a developer reading the database or this
 * source file cannot mistake it for a real survey of Fundacion Universitaria Konrad Lorenz
 * either. The flag is cleared, space by space, as real coordinates replace invented ones
 * through the ordinary {@code PUT} endpoints - see {@code SpaceService.update}.
 *
 * <p>Room {@code 302} is seeded with three deliberately mismatched aliases
 * ("sala de sistemas", "lab 302", "laboratorio 302") against the door name "Laboratorio de
 * Sistemas", because alias search is the feature most likely to look like it works while
 * being subtly broken, and this is the fixture every alias test in this service builds on.
 */
@ChangeUnit(id = "map-placeholder-campus-seed-v002", order = "002", author = "kapp")
public class V002_PlaceholderCampusSeed {

    private static final String CAMPUS = "Sede Principal";

    @Execution
    public void execute(MongoTemplate mongo) {
        Instant now = Instant.now();

        BuildingDocument bloqueA = mongo.save(new BuildingDocument(
                UUID.randomUUID().toString(),
                "A",
                "Bloque A",
                CAMPUS,
                "Main academic building. Classrooms, computer labs and faculty offices.",
                List.of(
                        floor(1, "/api/map/plans/bloque-a-p1.webp", 2048, 1536),
                        floor(2, "/api/map/plans/bloque-a-p2.webp", 2048, 1536),
                        floor(3, "/api/map/plans/bloque-a-p3.webp", 2048, 1536),
                        floor(4, "/api/map/plans/bloque-a-p4.webp", 2048, 1536),
                        floor(5, "/api/map/plans/bloque-a-p5.webp", 2048, 1536),
                        floor(6, "/api/map/plans/bloque-a-p6.webp", 2048, 1536),
                        floor(7, "/api/map/plans/bloque-a-p7.webp", 2048, 1536)),
                true,
                now,
                now));

        BuildingDocument bloqueB = mongo.save(new BuildingDocument(
                UUID.randomUUID().toString(),
                "B",
                "Bloque B",
                CAMPUS,
                "Psychology building. Research labs, library and student wellbeing services.",
                List.of(
                        floor(1, "/api/map/plans/bloque-b-p1.webp", 1920, 1440),
                        floor(2, "/api/map/plans/bloque-b-p2.webp", 1920, 1440),
                        floor(3, "/api/map/plans/bloque-b-p3.webp", 1920, 1440)),
                true,
                now,
                now));

        // The alias-hard fixture: door says "Laboratorio de Sistemas", students type any of
        // three other names. Every alias search test in this service leans on this room.
        space(mongo, bloqueA, "302", "Laboratorio de Sistemas", SpaceType.LAB, 3,
                List.of("sala de sistemas", "lab 302", "laboratorio 302"), 27.4, 55.8, 25, now);

        space(mongo, bloqueA, "612", "Aula 612", SpaceType.CLASSROOM, 6,
                List.of("sala 612", "salon 612"), 68.2, 31.9, 35, now);
        space(mongo, bloqueA, "708", "Aula 708", SpaceType.CLASSROOM, 7,
                List.of("sala 708", "salon 708"), 42.5, 63.1, 30, now);
        space(mongo, bloqueA, "709", "Aula 709", SpaceType.CLASSROOM, 7,
                List.of("sala 709", "salon 709"), 52.8, 63.1, 30, now);
        space(mongo, bloqueA, "710", "Aula 710", SpaceType.CLASSROOM, 7,
                List.of("sala 710", "salon 710"), 63.0, 63.1, 28, now);
        space(mongo, bloqueA, "711", "Aula 711", SpaceType.CLASSROOM, 7,
                List.of("sala 711", "salon 711"), 73.4, 63.1, 28, now);

        // A handful of non-classroom spaces in Bloque B, mostly so the seed is not
        // classrooms-only. "Cafeteria" is stored WITH its accent ("Cafetería") on
        // purpose: it is the cheapest natural fixture for "a search must match regardless
        // of how the query itself is accented" - searching the unaccented "cafeteria"
        // still has to find it.
        space(mongo, bloqueB, "101", "Biblioteca", SpaceType.LIBRARY, 1,
                List.of("sala de lectura"), 30.0, 40.0, null, now);
        space(mongo, bloqueB, "102", "Cafetería", SpaceType.CAFETERIA, 1,
                List.of("cafeteria bloque b"), 70.0, 45.0, null, now);
        space(mongo, bloqueB, "205", "Bienestar Universitario", SpaceType.WELLBEING, 2,
                List.of("bienestar", "psicologia"), 50.0, 50.0, null, now);
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        // Scoped to placeholder = true so a rerun never touches real data entered later
        // through the admin endpoints.
        mongo.remove(Query.query(Criteria.where("placeholder").is(true)), "spaces");
        mongo.remove(Query.query(Criteria.where("placeholder").is(true)), "buildings");
    }

    private static Floor floor(int level, String planImageUrl, int width, int height) {
        return new Floor(level, "Piso " + level, planImageUrl, width, height);
    }

    private static void space(MongoTemplate mongo, BuildingDocument building, String code,
                              String name, SpaceType type, int floorLevel, List<String> aliases,
                              double x, double y, Integer capacity, Instant now) {
        mongo.save(new SpaceDocument(
                UUID.randomUUID().toString(),
                code,
                name,
                type,
                building.id(),
                building.code(),
                building.campus(),
                floorLevel,
                aliases,
                x,
                y,
                capacity,
                true,
                now,
                now));
    }
}
