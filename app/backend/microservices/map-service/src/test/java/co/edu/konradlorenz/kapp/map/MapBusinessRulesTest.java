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
                .andExpect(jsonPath("$.floor.planImageUrl").value("/api/map/plans/bloque-a-p7.webp"))
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
                        new Floor(1, "Piso 1", "/test.png", 100, 100),
                        new Floor(2, "Piso 2", "/test.png", 100, 100)),
                false, now, now));
        spaces.save(new SpaceDocument(
                UUID.randomUUID().toString(), "OCCSP", "Occupied fixture space", SpaceType.OFFICE,
                occupied.id(), occupied.code(), occupied.campus(), 2, List.of(),
                10.0, 10.0, null, false, now, now));

        String bodyDroppingFloor2 = """
                {
                  "code": "OCC",
                  "name": "Occupied Floor Fixture",
                  "campus": "Sede Test",
                  "floors": [
                    {"level": 1, "name": "Piso 1", "planImageUrl": "/test.png", "imageWidth": 100, "imageHeight": 100}
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
                List.of(new Floor(1, "Piso 1", "/test.png", 100, 100)), false, now, now));
        BuildingDocument amb2 = buildings.save(new BuildingDocument(
                UUID.randomUUID().toString(), "AMB2", "Ambiguity Fixture 2", "Sede Test", null,
                List.of(new Floor(1, "Piso 1", "/test.png", 100, 100)), false, now, now));

        spaces.save(new SpaceDocument(
                UUID.randomUUID().toString(), "AMB", "Shared code, building 1", SpaceType.OFFICE,
                amb1.id(), amb1.code(), amb1.campus(), 1, List.of(), 10.0, 10.0, null, false, now, now));
        spaces.save(new SpaceDocument(
                UUID.randomUUID().toString(), "AMB", "Shared code, building 2", SpaceType.OFFICE,
                amb2.id(), amb2.code(), amb2.campus(), 1, List.of(), 10.0, 10.0, null, false, now, now));

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
}
