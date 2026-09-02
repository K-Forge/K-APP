package co.edu.konradlorenz.kapp.semaphore.migration;

import co.edu.konradlorenz.kapp.semaphore.domain.Curriculum;
import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumCourse;
import co.edu.konradlorenz.kapp.semaphore.repository.CurriculumRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the arithmetic {@code NEEDS_VERIFICATION.md} reports, so a future edit to the seed
 * that accidentally moves a course between levels is caught here instead of being
 * noticed by a student months later.
 *
 * <h2>Why these assertions do not equal the declared totals</h2>
 * The Ingenieria de Sistemas pensum was reconstructed from a printed diagram, and the
 * task that produced {@code curriculum-1015.json} was explicit: level assignments in
 * that reconstruction are sometimes wrong, and the fix is to report the discrepancy, not
 * to quietly adjust the seed until the numbers agree with the printed plan. So this test
 * asserts what the seed <strong>actually</strong> contains - the honest, currently-true
 * figures - and {@code NEEDS_VERIFICATION.md} is where each one is compared against the
 * declared reference and named as needing a human to correct it against the real
 * printed plan. If a fix changes a course's level, exactly one of these assertions fails
 * with the old number, which is the point: it forces the fix to also update this test
 * and the mismatch list together, rather than one silently drifting from the other.
 *
 * <p>{@code Curriculum.totalCredits()} and {@code totalHours()} are separately asserted
 * to still equal the printed plan's own declared 142 / 194 - see {@link Curriculum}'s
 * javadoc on why those fields are never recomputed from {@link #courses()}.
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class CurriculumSeedTotalsTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    private CurriculumRepository curricula;

    private Curriculum pensum1015() {
        return curricula.findById("1015").orElseThrow();
    }

    @Test
    @DisplayName("seed loads every reconstructed item of pensum 1015")
    void seedCourseCount() {
        // Roughly 51 per docs/api/semaphore.openapi.yaml's description of the real plan;
        // only 48 could be reconstructed from the printed diagram. See NEEDS_VERIFICATION.md.
        assertThat(pensum1015().courses()).hasSize(48);
    }

    @Test
    @DisplayName("header totals are kept verbatim from the printed plan, never recomputed")
    void declaredHeaderTotalsAreUnchanged() {
        Curriculum curriculum = pensum1015();
        assertThat(curriculum.totalCredits()).isEqualTo(142);
        assertThat(curriculum.totalHours()).isEqualTo(194);
        assertThat(curriculum.levels()).isEqualTo(9);
    }

    @Test
    @DisplayName("reconstructed items sum to 144 credits, 2 over the declared 142")
    void grandTotalCredits() {
        int sum = pensum1015().courses().stream().mapToInt(CurriculumCourse::credits).sum();
        assertThat(sum).isEqualTo(144);
    }

    @Test
    @DisplayName("reconstructed items sum to 197 weekly hours, 3 over the declared 194")
    void grandTotalWeeklyHours() {
        int sum = pensum1015().courses().stream().mapToInt(CurriculumCourse::weeklyHours).sum();
        assertThat(sum).isEqualTo(197);
    }

    @Test
    @DisplayName("level 1: 16 credits (matches declared 16), 22 hours (declared 21 - MISMATCH)")
    void level1Totals() {
        assertLevelTotals(1, 16, 22);
    }

    @Test
    @DisplayName("level 2: 16 credits (matches declared 16), 22 hours (matches declared 22)")
    void level2Totals() {
        assertLevelTotals(2, 16, 22);
    }

    @Test
    @DisplayName("level 3: 15 credits (declared 16 - MISMATCH), 21 hours (matches declared 21)")
    void level3Totals() {
        assertLevelTotals(3, 15, 21);
    }

    @Test
    @DisplayName("level 4: 15 credits (declared 16 - MISMATCH), 23 hours (matches declared 23)")
    void level4Totals() {
        assertLevelTotals(4, 15, 23);
    }

    @Test
    @DisplayName("level 5: 15 credits (declared 16 - MISMATCH), 20 hours (declared 21 - MISMATCH)")
    void level5Totals() {
        assertLevelTotals(5, 15, 20);
    }

    @Test
    @DisplayName("level 6: 18 credits (declared 16 - MISMATCH), 23 hours (declared 18 - MISMATCH)")
    void level6Totals() {
        assertLevelTotals(6, 18, 23);
    }

    @Test
    @DisplayName("level 7: 18 credits (declared 16 - MISMATCH), 25 hours (declared 19 - MISMATCH)")
    void level7Totals() {
        assertLevelTotals(7, 18, 25);
    }

    @Test
    @DisplayName("level 8: 17 credits (declared 16 - MISMATCH), 21 hours (declared 20 - MISMATCH)")
    void level8Totals() {
        assertLevelTotals(8, 17, 21);
    }

    @Test
    @DisplayName("level 9: 14 credits (matches declared 14), 20 hours (declared 29 - MISMATCH)")
    void level9Totals() {
        assertLevelTotals(9, 14, 20);
    }

    private void assertLevelTotals(int level, int expectedCredits, int expectedHours) {
        var itemsAtLevel = pensum1015().courses().stream().filter(c -> c.level() == level).toList();
        int credits = itemsAtLevel.stream().mapToInt(CurriculumCourse::credits).sum();
        int hours = itemsAtLevel.stream().mapToInt(CurriculumCourse::weeklyHours).sum();
        assertThat(credits).as("level %d credits", level).isEqualTo(expectedCredits);
        assertThat(hours).as("level %d weekly hours", level).isEqualTo(expectedHours);
    }
}
