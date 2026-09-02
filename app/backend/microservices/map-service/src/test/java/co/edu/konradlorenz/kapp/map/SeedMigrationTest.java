package co.edu.konradlorenz.kapp.map;

import co.edu.konradlorenz.kapp.map.domain.BuildingRepository;
import co.edu.konradlorenz.kapp.map.domain.SpaceDocument;
import co.edu.konradlorenz.kapp.map.domain.SpaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves {@code V002_PlaceholderCampusSeed} actually ran and produced what the migration
 * promises: two buildings, the real schedule-service room codes, the alias-hard fixture on
 * 302 exactly as specified, and - just as important - that {@code placeholder} never leaks
 * into a response. A client has no schema field to hide it behind; the only thing keeping it
 * off the wire is {@code MapMapper} translating storage documents into DTOs instead of
 * serialising them directly, and that is a regression a JSON diff would not necessarily
 * catch from the outside without an explicit assertion for it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class SeedMigrationTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BuildingRepository buildings;

    @Autowired
    private SpaceRepository spaces;

    private static RequestPostProcessor guest() {
        return jwt()
                .jwt(b -> b.subject("seed-guest").claim("roles", List.of("ROLE_GUEST")))
                .authorities(new SimpleGrantedAuthority("ROLE_GUEST"));
    }

    @Test
    @DisplayName("seeds exactly Bloque A and Bloque B, both flagged placeholder")
    void seedsExpectedBuildingCounts() {
        assertThat(buildings.count()).isEqualTo(2);
        assertThat(buildings.countByPlaceholderIsTrue()).isEqualTo(2);
        assertThat(buildings.existsByCode("A")).isTrue();
        assertThat(buildings.existsByCode("B")).isTrue();
    }

    @Test
    @DisplayName("seeds the real schedule-service room codes plus a few non-classroom spaces, all flagged placeholder")
    void seedsExpectedSpaceCounts() {
        // 302, 612, 708, 709, 710, 711 in Bloque A; 101, 102, 205 in Bloque B.
        assertThat(spaces.count()).isEqualTo(9);
        assertThat(spaces.countByPlaceholderIsTrue()).isEqualTo(9);

        List<String> codes = spaces.findAll().stream().map(SpaceDocument::code).sorted().toList();
        assertThat(codes).containsExactlyInAnyOrder(
                "302", "612", "708", "709", "710", "711", "101", "102", "205");
    }

    @Test
    @DisplayName("room 302 carries the exact mismatched-alias fixture the brief specified")
    void room302HasTheSpecifiedAliases() {
        SpaceDocument room302 = spaces.findByCodeOrderByBuildingCodeAsc("302").get(0);

        assertThat(room302.name()).isEqualTo("Laboratorio de Sistemas");
        assertThat(room302.aliases()).containsExactly(
                "sala de sistemas", "lab 302", "laboratorio 302");
    }

    @Test
    @DisplayName("the placeholder flag never appears in an API response")
    void placeholderFlagNeverLeaksToTheApi() throws Exception {
        mockMvc.perform(get("/api/map/buildings/A").with(guest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeholder").doesNotExist());

        mockMvc.perform(get("/api/map/spaces/302").with(guest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeholder").doesNotExist());
    }
}
