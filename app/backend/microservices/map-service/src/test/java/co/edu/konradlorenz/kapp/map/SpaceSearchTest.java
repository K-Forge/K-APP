package co.edu.konradlorenz.kapp.map;

import co.edu.konradlorenz.kapp.map.domain.BuildingDocument;
import co.edu.konradlorenz.kapp.map.domain.BuildingRepository;
import co.edu.konradlorenz.kapp.map.domain.Floor;
import co.edu.konradlorenz.kapp.map.domain.SpaceDocument;
import co.edu.konradlorenz.kapp.map.domain.SpaceRepository;
import co.edu.konradlorenz.kapp.map.domain.SpaceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Search is the feature the whole service exists to serve, and the part most likely to look
 * like it works while being subtly broken - see {@code SpaceSearch}'s class-level javadoc for
 * why a case-insensitive regex is not good enough here. These tests exercise the real thing
 * against a real {@code mongo:7.0} text index, not a mock.
 *
 * <p>Reads from two sources of fixtures: the placeholder campus that
 * {@code V002_PlaceholderCampusSeed} loads into every fresh database (room 302's mismatched
 * aliases, and "Cafeteria" for the accent case), and a small dedicated building this class
 * inserts itself for the one case the seed cannot demonstrate cleanly - ranking - because a
 * ranking assertion needs to know with certainty that nothing else in the collection
 * coincidentally outscores the fixture.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class SpaceSearchTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BuildingRepository buildings;

    @Autowired
    private SpaceRepository spaces;

    private static final String RANK_CODE_EXACT = "999";
    private static final String RANK_CODE_MENTION = "ZZ1";

    @BeforeAll
    void seedRankingFixture() {
        Instant now = Instant.now();
        BuildingDocument rankBuilding = buildings.save(new BuildingDocument(
                UUID.randomUUID().toString(),
                "RANK",
                "Ranking fixture building",
                "Sede Test",
                null,
                List.of(new Floor(1, "Piso 1", "/test.png", 100, 100)),
                false,
                now,
                now));

        // Matches "999" through its code (text index weight 5).
        spaces.save(new SpaceDocument(
                UUID.randomUUID().toString(), RANK_CODE_EXACT, "Cuarto generico",
                SpaceType.OTHER, rankBuilding.id(), rankBuilding.code(), rankBuilding.campus(),
                1, List.of(), 10.0, 10.0, null, false, now, now));

        // Matches "999" only because its NAME happens to mention the other room (weight 3),
        // never through its own code. Ranking must still put RANK_CODE_EXACT first.
        spaces.save(new SpaceDocument(
                UUID.randomUUID().toString(), RANK_CODE_MENTION, "Ver salon 999 para informacion",
                SpaceType.OTHER, rankBuilding.id(), rankBuilding.code(), rankBuilding.campus(),
                1, List.of(), 20.0, 20.0, null, false, now, now));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor guest() {
        return jwt()
                .jwt(b -> b.subject("search-guest").claim("roles", List.of("ROLE_GUEST")))
                .authorities(new SimpleGrantedAuthority("ROLE_GUEST"));
    }

    @Test
    @DisplayName("search is case- and accent-insensitive: lowercase unaccented 'cafeteria' finds stored 'Cafeteria'")
    void caseAndAccentInsensitive() throws Exception {
        // The query is typed all-lowercase and without the accent the stored name actually
        // carries ("Cafeteria"), exercising both dimensions in one request.
        mockMvc.perform(get("/api/map/spaces/search").param("q", "cafeteria").with(guest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("102"))
                .andExpect(jsonPath("$.content[0].name").value("Cafetería"));
    }

    @Test
    @DisplayName("alias search: 'sala de sistemas' finds room 302 (door name 'Laboratorio de Sistemas') and ranks it first")
    void aliasSearchFindsRoomBehindMismatchedDoorName() throws Exception {
        // Several other seeded rooms also carry a "sala NNN" alias, so more than one result
        // is expected back; the point of the test is that the alias hit for 302 - which
        // matches BOTH "sala" and "sistemas" - outranks every space that only matches "sala".
        mockMvc.perform(get("/api/map/spaces/search").param("q", "sala de sistemas").with(guest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("302"))
                .andExpect(jsonPath("$.content[0].name").value("Laboratorio de Sistemas"));
    }

    @Test
    @DisplayName("exact code match outranks a space that only mentions the code in its name")
    void exactCodeMatchRanksFirst() throws Exception {
        mockMvc.perform(get("/api/map/spaces/search").param("q", RANK_CODE_EXACT).with(guest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(RANK_CODE_EXACT))
                .andExpect(jsonPath("$.content[0].name").value("Cuarto generico"));
    }

    @Test
    @DisplayName("page size above the documented cap of 100 is rejected with 400, never silently clamped")
    void oversizedPageIsRejected() throws Exception {
        mockMvc.perform(get("/api/map/spaces/search")
                        .param("q", "aula")
                        .param("size", "101")
                        .with(guest()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("an empty result page is a normal 200, not an error")
    void noMatchIsAnEmptyPageNotAnError() throws Exception {
        mockMvc.perform(get("/api/map/spaces/search").param("q", "xyznomatch").with(guest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }
}
