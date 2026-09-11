package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.semaphore.domain.CourseStatus;
import co.edu.konradlorenz.kapp.semaphore.domain.Pensum;
import co.edu.konradlorenz.kapp.semaphore.domain.PensumArea;
import co.edu.konradlorenz.kapp.semaphore.domain.PensumCourse;
import co.edu.konradlorenz.kapp.semaphore.domain.PensumStatus;
import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgress;
import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgressCourse;
import co.edu.konradlorenz.kapp.semaphore.repository.StudentProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for reconciling a pinned progress document against the pensum it
 * names: what gets added, what gets kept-but-reported as removed, and that an unchanged
 * document is never rewritten.
 */
@ExtendWith(MockitoExtension.class)
class PensumReconcilerTest {

    @Mock
    private StudentProgressRepository progressRepository;

    // Built in @BeforeEach, not as a field initializer: MockitoExtension injects @Mock
    // fields via a post-processor that runs after instance construction, so a field
    // initializer here would still see progressRepository as null.
    private PensumReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new PensumReconciler(progressRepository);
    }

    @Test
    @DisplayName("an item the pensum gained is materialised as PENDING and reported as added")
    void addsAndReportsNewPensumItems() {
        Pensum pensum = pensum(List.of(
                course("10011", "1001", 1), course("20015", "1004", 1)));
        StudentProgress progress = progress(List.of(passed("10011", "1001")));
        when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PensumReconciler.Result result = reconciler.reconcile(progress, pensum);

        assertThat(result.reconciliation().addedCourses()).containsExactly("20015");
        assertThat(result.reconciliation().removedCourses()).isEmpty();
        assertThat(result.reconciliation().inSync()).isFalse();
        assertThat(result.progress().findByPensumItemCode("1004"))
                .hasValueSatisfying(entry -> assertThat(entry.status()).isEqualTo(CourseStatus.PENDING));
        verify(progressRepository).save(any());
    }

    @Test
    @DisplayName("an entry whose pensum item was dropped is kept, not deleted, and reported as removed")
    void keepsAndReportsRemovedEntries() {
        Pensum pensum = pensum(List.of(course("10011", "1001", 1)));
        StudentProgressCourse droppedButPassed = passed("20015", "1004");
        StudentProgress progress = progress(List.of(passed("10011", "1001"), droppedButPassed));
        when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PensumReconciler.Result result = reconciler.reconcile(progress, pensum);

        assertThat(result.reconciliation().removedCourses()).containsExactly("20015");
        assertThat(result.reconciliation().addedCourses()).isEmpty();
        // The grade is never destroyed: the entry is still in courses().
        assertThat(result.progress().findByPensumItemCode("1004")).contains(droppedButPassed);
    }

    @Test
    @DisplayName("a document that already mirrors its pensum is returned unchanged and never rewritten")
    void noOpWhenAlreadyInSync() {
        Pensum pensum = pensum(List.of(course("10011", "1001", 1)));
        StudentProgress progress = progress(List.of(passed("10011", "1001")));

        PensumReconciler.Result result = reconciler.reconcile(progress, pensum);

        assertThat(result.reconciliation().inSync()).isTrue();
        assertThat(result.progress()).isSameAs(progress);
        verify(progressRepository, never()).save(any());
    }

    private static Pensum pensum(List<PensumCourse> courses) {
        return new Pensum("1015", "506", "Ingenieria de Sistemas", "Facultad",
                "Reforma 2018", PensumStatus.ACTIVE, 142, 194, 9,
                List.of(new PensumArea("CB", "Ciencias Basicas", "#539392", 36, 44)), courses);
    }

    private static PensumCourse course(String code, String pensumItemCode, int level) {
        return new PensumCourse(code, pensumItemCode, "Course " + code, level, 3, 4, "CB",
                false, List.of(), null);
    }

    private static StudentProgress progress(List<StudentProgressCourse> courses) {
        return new StudentProgress("id1", "user1", "506232730", "506", "1015", 1, courses, Instant.now());
    }

    private static StudentProgressCourse passed(String code, String pensumItemCode) {
        return new StudentProgressCourse(code, pensumItemCode, CourseStatus.PASSED, "20221", 45, null, null);
    }
}
