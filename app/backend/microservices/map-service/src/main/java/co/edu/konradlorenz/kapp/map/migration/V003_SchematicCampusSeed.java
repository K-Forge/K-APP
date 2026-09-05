package co.edu.konradlorenz.kapp.map.migration;

import co.edu.konradlorenz.kapp.map.domain.BuildingDocument;
import co.edu.konradlorenz.kapp.map.domain.Corridor;
import co.edu.konradlorenz.kapp.map.domain.Floor;
import co.edu.konradlorenz.kapp.map.domain.GridPoint;
import co.edu.konradlorenz.kapp.map.domain.SpaceDocument;
import co.edu.konradlorenz.kapp.map.domain.SpaceType;
import co.edu.konradlorenz.kapp.map.domain.Wing;
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
 * <p>Replaces the pinned-on-a-photograph seed with a schematic one, and removes whatever the
 * old model left behind so an existing volume converges on the same state as a fresh one.
 *
 * <p>The real campus is five buildings, the central one with eight floors and a basement -
 * roughly forty floors in all. None of that is here, because none of it has been surveyed
 * yet. What is here is a small campus that is <em>structurally</em> complete: it exercises
 * every part of the new model, so the mobile clients can be built against something that
 * behaves like the real thing rather than something that merely has rooms in it.
 *
 * <p>Specifically it covers:
 * <ul>
 *   <li>a <strong>basement at level -1</strong>, where the rule that a room's first digit is
 *       its floor stops applying;</li>
 *   <li>the three <strong>wings</strong> - {@code 301}, {@code 301-N} and {@code 301-S} are
 *       three different rooms on one floor, which is the case most likely to send a student
 *       to the wrong door;</li>
 *   <li><strong>corridors</strong> with the colours a floor is actually painted;</li>
 *   <li><strong>{@code accessVia}</strong>, so "piso 3, sube por el ascensor central" is
 *       answerable from stored data rather than guessed from grid distance.</li>
 * </ul>
 *
 * <p>Every document carries {@code placeholder = true}. The flag is storage-only -
 * {@code MapMapper} never puts it on a response - so a client cannot brand it "seed data",
 * but a developer reading the database cannot mistake it for a survey either. It clears
 * space by space as real data replaces invented data through the ordinary {@code PUT}
 * endpoints.
 *
 * <p>Room {@code 302} keeps its three deliberately mismatched aliases against the door name
 * "Laboratorio de Sistemas": alias search is the feature most likely to look like it works
 * while being subtly broken, and every alias test in this service builds on that fixture.
 */
@ChangeUnit(id = "map-schematic-campus-seed-v003", order = "003", author = "kapp")
public class V003_SchematicCampusSeed {

    private static final String CAMPUS = "Sede Principal";

    /** Corridor colours as the wings are actually painted, not a palette invented here. */
    private static final String NORTE_COLOR = "#F2A93B";
    private static final String SUR_COLOR = "#4C9F70";
    private static final String CENTRAL_COLOR = "#5B8DEF";

    @Execution
    public void execute(MongoTemplate mongo) {
        // Everything the superseded seed wrote, in a shape that no longer has a meaning.
        // Scoped to placeholder = true so real data entered through the admin endpoints is
        // never touched.
        mongo.remove(Query.query(Criteria.where("placeholder").is(true)), "spaces");
        mongo.remove(Query.query(Criteria.where("placeholder").is(true)), "buildings");

        Instant now = Instant.now();

        BuildingDocument bloqueA = mongo.save(new BuildingDocument(
                UUID.randomUUID().toString(),
                "A",
                "Bloque A",
                CAMPUS,
                "Main academic building. Classrooms, computer labs and faculty offices.",
                List.of(
                        basement(),
                        floor(1, "Piso 1"),
                        floor(2, "Piso 2"),
                        floor(3, "Piso 3"),
                        floor(6, "Piso 6"),
                        floor(7, "Piso 7")),
                true,
                now,
                now));

        BuildingDocument bloqueB = mongo.save(new BuildingDocument(
                UUID.randomUUID().toString(),
                "B",
                "Bloque B",
                CAMPUS,
                "Psychology building. Research labs, library and student wellbeing services.",
                List.of(floor(1, "Piso 1"), floor(2, "Piso 2"), floor(3, "Piso 3")),
                true,
                now,
                now));

        // ── Circulation first: everything else points at it through accessVia ───────────
        space(mongo, bloqueA, "ASC-CENTRAL", "Ascensor central", SpaceType.ELEVATOR, 1,
                List.of("ascensor", "elevador"), 5, 7, 1, 1, null, null, now);
        space(mongo, bloqueA, "ESC-NORTE", "Escaleras norte", SpaceType.STAIRS, 1,
                List.of("escalera norte"), 1, 2, 1, 1, null, null, now);
        space(mongo, bloqueA, "ESC-SUR", "Escaleras sur", SpaceType.STAIRS, 1,
                List.of("escalera sur"), 9, 2, 1, 1, null, null, now);
        space(mongo, bloqueA, "ENT-PRINCIPAL", "Entrada principal", SpaceType.ENTRANCE, 1,
                List.of("entrada", "porteria"), 5, 0, 1, 1, null, null, now);

        // ── The three wings, which is the case that matters ────────────────────────────
        // Same base code, three different rooms on one floor. A student told "salón 301"
        // without the wing is standing in the wrong place two thirds of the time.
        space(mongo, bloqueA, "301", "Aula 301", SpaceType.CLASSROOM, 3,
                List.of("salon 301"), 5, 4, 1, 2, "ASC-CENTRAL", 30, now);
        space(mongo, bloqueA, "301-N", "Aula 301 Norte", SpaceType.CLASSROOM, 3,
                List.of("salon 301 norte"), 1, 4, 1, 2, "ESC-NORTE", 28, now);
        space(mongo, bloqueA, "301-S", "Aula 301 Sur", SpaceType.CLASSROOM, 3,
                List.of("salon 301 sur"), 9, 4, 1, 2, "ESC-SUR", 28, now);

        // ── The room codes schedule-service resolves, carried over unchanged ───────────
        // The alias-hard fixture: door says "Laboratorio de Sistemas", students type any of
        // three other names. Every alias search test in this service leans on this room.
        space(mongo, bloqueA, "302", "Laboratorio de Sistemas", SpaceType.LAB, 3,
                List.of("sala de sistemas", "lab 302", "laboratorio 302"),
                5, 7, 2, 3, "ASC-CENTRAL", 25, now);

        space(mongo, bloqueA, "612", "Aula 612", SpaceType.CLASSROOM, 6,
                List.of("sala 612", "salon 612"), 3, 6, 1, 2, "ASC-CENTRAL", 35, now);
        space(mongo, bloqueA, "708", "Aula 708", SpaceType.CLASSROOM, 7,
                List.of("sala 708", "salon 708"), 3, 4, 1, 2, "ASC-CENTRAL", 30, now);
        space(mongo, bloqueA, "709", "Aula 709", SpaceType.CLASSROOM, 7,
                List.of("sala 709", "salon 709"), 3, 7, 1, 2, "ASC-CENTRAL", 30, now);
        space(mongo, bloqueA, "710", "Aula 710", SpaceType.CLASSROOM, 7,
                List.of("sala 710", "salon 710"), 7, 4, 1, 2, "ASC-CENTRAL", 28, now);
        space(mongo, bloqueA, "711", "Aula 711", SpaceType.CLASSROOM, 7,
                List.of("sala 711", "salon 711"), 7, 7, 1, 2, "ASC-CENTRAL", 28, now);

        // A room that spans several cells, so the client has to honour rowSpan/colSpan
        // rather than assuming every space is one square.
        space(mongo, bloqueA, "310", "Auditorio Konrad Lorenz", SpaceType.AUDITORIUM, 3,
                List.of("auditorio"), 7, 11, 3, 4, "ASC-CENTRAL", 180, now);

        // ── The basement, where the first-digit rule does not apply ────────────────────
        space(mongo, bloqueA, "S-01", "Parqueadero", SpaceType.OTHER, -1,
                List.of("parqueadero", "sotano"), 2, 2, 4, 6, "ASC-CENTRAL", null, now);
        space(mongo, bloqueA, "S-02", "Depósito", SpaceType.OTHER, -1,
                List.of("deposito", "bodega"), 7, 2, 1, 2, "ASC-CENTRAL", null, now);

        // ── Bloque B, codes carried over unchanged ─────────────────────────────────────
        space(mongo, bloqueB, "B-ASC", "Ascensor Bloque B", SpaceType.ELEVATOR, 1,
                List.of("ascensor bloque b"), 4, 5, 1, 1, null, null, now);
        space(mongo, bloqueB, "101", "Biblioteca", SpaceType.LIBRARY, 1,
                List.of("sala de lectura"), 2, 2, 2, 3, "B-ASC", null, now);
        // Stored WITH its accent on purpose: it is the cheapest natural fixture for "a
        // search must match regardless of how the query itself is accented" - the unaccented
        // "cafeteria" still has to find it.
        space(mongo, bloqueB, "102", "Cafetería", SpaceType.CAFETERIA, 1,
                List.of("cafeteria bloque b"), 6, 2, 2, 3, "B-ASC", null, now);
        space(mongo, bloqueB, "205", "Bienestar Universitario", SpaceType.WELLBEING, 2,
                List.of("bienestar", "psicologia"), 4, 3, 1, 2, "B-ASC", null, now);
        space(mongo, bloqueB, "210", "Terraza", SpaceType.TERRACE, 2,
                List.of("terraza"), 1, 7, 2, 3, "B-ASC", null, now);
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.remove(Query.query(Criteria.where("placeholder").is(true)), "spaces");
        mongo.remove(Query.query(Criteria.where("placeholder").is(true)), "buildings");
    }

    /**
     * The basement is level -1, not level 0. Level 0 would read as a ground floor, which is
     * a different thing that this building does not have - its ground floor is level 1.
     */
    private static Floor basement() {
        return new Floor(-1, "Sótano", 10, 10, List.of(
                new Corridor("PAS-S-CENTRAL", "Pasillo sótano", CENTRAL_COLOR,
                        List.of(new GridPoint(6, 0), new GridPoint(6, 9)))));
    }

    private static Floor floor(int level, String name) {
        return new Floor(level, name, 11, 16, List.of(
                new Corridor("PAS-CENTRAL", "Pasillo central", CENTRAL_COLOR,
                        List.of(new GridPoint(5, 0), new GridPoint(5, 15))),
                new Corridor("PAS-NORTE", "Pasillo norte", NORTE_COLOR,
                        List.of(new GridPoint(5, 3), new GridPoint(1, 3), new GridPoint(1, 15))),
                new Corridor("PAS-SUR", "Pasillo sur", SUR_COLOR,
                        List.of(new GridPoint(5, 3), new GridPoint(9, 3), new GridPoint(9, 15)))));
    }

    private static void space(MongoTemplate mongo, BuildingDocument building, String code,
                              String name, SpaceType type, int floorLevel, List<String> aliases,
                              int gridRow, int gridColumn, int rowSpan, int colSpan,
                              String accessVia, Integer capacity, Instant now) {
        mongo.save(new SpaceDocument(
                UUID.randomUUID().toString(),
                code,
                SpaceDocument.baseCodeOf(code),
                wingFor(code),
                name,
                type,
                building.id(),
                building.code(),
                building.campus(),
                floorLevel,
                aliases,
                gridRow,
                gridColumn,
                rowSpan,
                colSpan,
                accessVia,
                capacity,
                true,
                now,
                now));
    }

    /**
     * {@code S-01} and {@code ESC-SUR} both end in something after a dash, and neither is a
     * wing. Only the single-letter suffixes N, S and C name one, which is what
     * {@link SpaceDocument#wingOf} checks.
     */
    private static Wing wingFor(String code) {
        return SpaceDocument.wingOf(code);
    }
}
