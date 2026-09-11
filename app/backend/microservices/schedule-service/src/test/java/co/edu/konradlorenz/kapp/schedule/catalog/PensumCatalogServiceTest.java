package co.edu.konradlorenz.kapp.schedule.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Plain Mockito unit test - no Spring context, no MongoDB - for the one behaviour that
 * matters about the semaphore-service dependency: this service must still work when that
 * one is down. {@link CatalogClient} is mocked directly here rather than through Spring,
 * the way the task calls for.
 */
@ExtendWith(MockitoExtension.class)
class PensumCatalogServiceTest {

    @Mock
    private CatalogClient client;

    @Test
    @DisplayName("a successful catalogue read is returned as-is")
    void pensumCourses_success_returnsTheCatalogue() {
        PensumCourseView course = new PensumCourseView("17080", "2018", "ESTADISTICA DESCRIPTIVA", 6, 3);
        when(client.listPensumCourses("1015")).thenReturn(List.of(course));

        PensumCatalogService service = new PensumCatalogService(client);

        assertThat(service.pensumCourses("1015")).containsExactly(course);
    }

    @Test
    @DisplayName("a Feign failure fails open: empty list, not an exception")
    void pensumCourses_clientThrows_returnsEmptyRatherThanPropagating() {
        when(client.listPensumCourses("1015")).thenThrow(new RuntimeException("semaphore-service is down"));

        PensumCatalogService service = new PensumCatalogService(client);

        assertThat(service.pensumCourses("1015")).isEmpty();
    }

    @Test
    @DisplayName("find matches a fixed course by its course code")
    void find_matchesByCourseCode() {
        PensumCourseView course = new PensumCourseView("17080", "2018", "ESTADISTICA DESCRIPTIVA", 6, 3);
        when(client.listPensumCourses("1015")).thenReturn(List.of(course));

        PensumCatalogService service = new PensumCatalogService(client);

        assertThat(service.find("1015", "17080", "2018")).contains(course);
    }

    @Test
    @DisplayName("find matches an elective slot, which has no course code, by pensumItemCode")
    void find_matchesElectiveSlotByPensumItemCode() {
        PensumCourseView elective = new PensumCourseView(null, "ELECTIVA_VI", "Electiva VI", 9, 3);
        when(client.listPensumCourses("1015")).thenReturn(List.of(elective));

        PensumCatalogService service = new PensumCatalogService(client);

        assertThat(service.find("1015", "99999", "ELECTIVA_VI")).contains(elective);
    }

    @Test
    @DisplayName("find returns empty when the course is genuinely not in the pensum")
    void find_noMatch_returnsEmpty() {
        when(client.listPensumCourses("1015")).thenReturn(List.of());

        PensumCatalogService service = new PensumCatalogService(client);

        assertThat(service.find("1015", "00000", "0000")).isEmpty();
    }
}
