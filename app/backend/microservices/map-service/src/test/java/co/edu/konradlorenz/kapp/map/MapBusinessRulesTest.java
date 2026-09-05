package co.edu.konradlorenz.kapp.map;

import co.edu.konradlorenz.kapp.map.domain.BuildingDocument;
import co.edu.konradlorenz.kapp.map.domain.BuildingRepository;
import co.edu.konradlorenz.kapp.map.domain.Floor;
import co.edu.konradlorenz.kapp.map.domain.SpaceDocument;
import co.edu.konradlorenz.kapp.map.domain.SpaceRepository;
import co.edu.konradlorenz.kapp.map.domain.SpaceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The domain rules that are easy to get backwards: a cascade instead of a refusal, a
 * two-call round trip where the contract promises one, an ambiguous code silently resolved
 * to the wrong building. Each of these was called out explicitly in the brief because
 * getting it wrong is data loss or a wrong answer, not a crash.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class MapBusinessRulesTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BuildingRepository buildings;

    @Autowired
    private SpaceRepository spaces;

    private static RequestPostProcessor admin() {
        return jwt()
                .jwt(b -> b.subject("rules-admin").claim("roles", List.of("ROLE_ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor guest() {
        return jwt()
                .jwt(b -> b.subject("rules-guest").claim("roles", List.of("ROLE_GUEST")))
                .authorities(new SimpleGrantedAuthority("ROLE_GUEST"));
    }

    @Test
    @DisplayName("GET /api/map/spaces/{code} returns the space, its floor and its building in one call")
    void spaceDetailComposesSpaceFloorAndBuilding() throws Exception {
        mockMvc.perform(get("/api/map/spaces/708").with(guest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("708"))
                .andExpect(jsonPath("$.buildingCode").value("A"))
                .andExpect(jsonPath("$.floor.level").value(7))
                .andExpect(jsonPath("$.floor.gridRows").value(11))
                .andExpect(jsonPath("$.floor.gridColumns").value(16))
                .andExpect(jsonPath("$.building.code").value("A"))
                .andExpect(jsonPath("$.building.name").value("Bloque A"));
    }

    @Test
    @DisplayName("deleting a building that still has spaces is refused with 409 and nothing is deleted")
    void deletingOccupiedBuildingIsConflict() throws Exception {
        mockMvc.perform(delete("/api/map/buildings/A").with(admin()))
                .andExpect(status().isConflict());

        assertThat(buildings.existsByCode("A"))
                .as("the building must survive the refused delete")
                .isTrue();
        assertThat(spaces.findByBuildingId(buildings.findByCode("A").orElseThrow().id()))
                .as("none of its spaces were cascaded away")
                .isNotEmpty();
    }

    @Test
    @DisplayName("dropping a floor that still has spaces on it is refused with 409")
    void droppingOccupiedFloorIsConflict() throws Exception {
        Instant now = Instant.now();
        BuildingDocument occupied = buildings.save(new BuildingDocument(
                UUID.randomUUID().toString(), "OCC", "Occupied Floor Fixture", "Sede Test", null,
                List.of(
                        new Floor(1, "Piso 1", 10, 10, List.of()),
                        new Floor(2, "Piso 2", 10, 10, List.of())),
                false, now, now));
        spaces.save(new SpaceDocument(
                UUID.randomUUID().toString(),
                "OCCSP",
                SpaceDocument.baseCodeOf("OCCSP"),
                SpaceDocument.wingOf("OCCSP"),
                "Occupied fixture space",
                SpaceType.OFFICE,
                occupied.id(),
                occupied.code(),
                occupied.campus(),
                2,
                List.of(),
                1,
                1,
                1,
                1,
                null,
                null,
                false,
                now,
                now));

        String bodyDroppingFloor2 = """
                {
                  "code": "OCC",
                  "name": "Occupied Floor Fixture",
                  "campus": "Sede Test",
                  "floors": [
                    {"level": 1, "name": "Piso 1", "gridRows": 10, "gridColumns": 10}
                  ]
                }
                """;

        mockMvc.perform(put("/api/map/buildings/OCC").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyDroppingFloor2))
                .andExpect(status().isConflict());

        assertThat(buildings.findByCode("OCC").orElseThrow().floors())
                .as("the floor was not actually dropped")
                .hasSize(2);
    }

    @Test
    @DisplayName("a room code shared by two buildings is refused with 409 listing the candidate buildings")
    void ambiguousRoomCodeListsCandidates() throws Exception {
        Instant now = Instant.now();
        BuildingDocument amb1 = buildings.save(new BuildingDocument(
                UUID.randomUUID().toString(), "AMB1", "Ambiguity Fixture 1", "Sede Test", null,
                List.of(new Floor(1, "Piso 1", 10, 10, List.of())), false, now, now));
        BuildingDocument amb2 = buildings.save(new BuildingDocument(
                UUID.randomUUID().toString(), "AMB2", "Ambiguity Fixture 2", "Sede Test", null,
                List.of(new Floor(1, "Piso 1", 10, 10, List.of())), false, now, now));

        spaces.save(new SpaceDocument(
                UUID.randomUUID().toString(),
                "AMB",
                SpaceDocument.baseCodeOf("AMB"),
                SpaceDocument.wingOf("AMB"),
                "Shared code, building 1",
                SpaceType.OFFICE,
                amb1.id(),
                amb1.code(),
                amb1.campus(),
                1,
                List.of(),
                1,
                1,
                1,
                1,
                null,
                null,
                false,
                now,
                now));
        spaces.save(new SpaceDocument(
                UUID.randomUUID().toString(),
                "AMB",
                SpaceDocument.baseCodeOf("AMB"),
                SpaceDocument.wingOf("AMB"),
                "Shared code, building 2",
                SpaceType.OFFICE,
                amb2.id(),
                amb2.code(),
                amb2.campus(),
                1,
                List.of(),
                1,
                1,
                1,
                1,
                null,
                null,
                false,
                now,
                now));

        mockMvc.perform(get("/api/map/spaces/AMB").with(guest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details[0].field").value("buildingCode"))
                .andExpect(jsonPath("$.details[0].issue").value("Candidate building: AMB1"))
                .andExpect(jsonPath("$.details[1].issue").value("Candidate building: AMB2"));

        // Disambiguated, the same code resolves cleanly.
        mockMvc.perform(get("/api/map/spaces/AMB").param("buildingCode", "AMB2").with(guest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.building.code").value("AMB2"));
    }

    // ── The schematic model's own rules ────────────────────────────────────────────

    @Test
    @DisplayName("a space that would not fit on its floor's grid is refused with 400")
    void spaceOutsideTheGridIsRejected() throws Exception {
        // Floor 3 of Bloque A is 11 x 16, so column 20 does not exist.
        mockMvc.perform(post("/api/map/spaces").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "OUT-1",
                                  "name": "Fuera de la rejilla",
                                  "type": "CLASSROOM",
                                  "buildingCode": "A",
                                  "floorLevel": 3,
                                  "gridRow": 2,
                                  "gridColumn": 20
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("gridRow"));
    }

    @Test
    @DisplayName("a space whose span runs off the edge is refused, not silently clipped")
    void spanRunningOffTheEdgeIsRejected() throws Exception {
        mockMvc.perform(post("/api/map/spaces").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "OUT-2",
                                  "name": "Se sale por el borde",
                                  "type": "AUDITORIUM",
                                  "buildingCode": "A",
                                  "floorLevel": 3,
                                  "gridRow": 2,
                                  "gridColumn": 14,
                                  "colSpan": 6
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("two spaces cannot occupy the same cell: the second would be invisible, not obviously wrong")
    void overlappingSpacesAreRefused() throws Exception {
        // Room 301 sits at row 5, columns 4-5 of floor 3.
        mockMvc.perform(post("/api/map/spaces").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "OVER-1",
                                  "name": "Encima de 301",
                                  "type": "CLASSROOM",
                                  "buildingCode": "A",
                                  "floorLevel": 3,
                                  "gridRow": 5,
                                  "gridColumn": 5
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details[0].issue").value("301"));
    }

    @Test
    @DisplayName("the same cell on a different floor is fine")
    void sameCellOnAnotherFloorIsAllowed() throws Exception {
        mockMvc.perform(post("/api/map/spaces").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "OK-1",
                                  "name": "Mismo lugar, otro piso",
                                  "type": "CLASSROOM",
                                  "buildingCode": "A",
                                  "floorLevel": 2,
                                  "gridRow": 5,
                                  "gridColumn": 4,
                                  "colSpan": 2
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("a space in the basement is accepted: level -1 is a floor like any other")
    void basementSpacesAreAccepted() throws Exception {
        mockMvc.perform(post("/api/map/spaces").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "S-99",
                                  "name": "Cuarto de máquinas",
                                  "type": "OTHER",
                                  "buildingCode": "A",
                                  "floorLevel": -1,
                                  "gridRow": 0,
                                  "gridColumn": 0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.floorLevel").value(-1));
    }

    @Test
    @DisplayName("the wing is derived from a -N suffix when the caller does not send one")
    void wingIsDerivedFromTheCode() throws Exception {
        mockMvc.perform(post("/api/map/spaces").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "205-N",
                                  "name": "Aula 205 Norte",
                                  "type": "CLASSROOM",
                                  "buildingCode": "A",
                                  "floorLevel": 2,
                                  "gridRow": 1,
                                  "gridColumn": 8
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wing").value("NORTE"))
                .andExpect(jsonPath("$.baseCode").value("205"));
    }

    @Test
    @DisplayName("an explicit wing wins over what the code implies")
    void explicitWingWins() throws Exception {
        mockMvc.perform(post("/api/map/spaces").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "206",
                                  "name": "Aula 206",
                                  "type": "CLASSROOM",
                                  "buildingCode": "A",
                                  "floorLevel": 2,
                                  "gridRow": 8,
                                  "gridColumn": 8,
                                  "wing": "CENTRAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wing").value("CENTRAL"));
    }

    @Test
    @DisplayName("accessVia must name a real circulation element, or the app tells a visitor to use a lift that is not there")
    void accessViaMustResolve() throws Exception {
        mockMvc.perform(post("/api/map/spaces").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "AV-1",
                                  "name": "Se llega por un ascensor inventado",
                                  "type": "CLASSROOM",
                                  "buildingCode": "A",
                                  "floorLevel": 2,
                                  "gridRow": 0,
                                  "gridColumn": 0,
                                  "accessVia": "ASC-QUE-NO-EXISTE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("accessVia"));
    }

    @Test
    @DisplayName("accessVia may not point at an ordinary room: nobody travels through a classroom")
    void accessViaMustBeCirculation() throws Exception {
        mockMvc.perform(post("/api/map/spaces").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "AV-2",
                                  "name": "Se llega por otro salón",
                                  "type": "CLASSROOM",
                                  "buildingCode": "A",
                                  "floorLevel": 2,
                                  "gridRow": 0,
                                  "gridColumn": 2,
                                  "accessVia": "302"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].issue").value("must be an ELEVATOR, STAIRS or ENTRANCE"));
    }

    @Test
    @DisplayName("a real lift is accepted, including from another floor of the same building")
    void accessViaAcceptsARealLift() throws Exception {
        mockMvc.perform(post("/api/map/spaces").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "AV-3",
                                  "name": "Se llega por el ascensor central",
                                  "type": "CLASSROOM",
                                  "buildingCode": "A",
                                  "floorLevel": 2,
                                  "gridRow": 0,
                                  "gridColumn": 4,
                                  "accessVia": "ASC-CENTRAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessVia").value("ASC-CENTRAL"));
    }
}
