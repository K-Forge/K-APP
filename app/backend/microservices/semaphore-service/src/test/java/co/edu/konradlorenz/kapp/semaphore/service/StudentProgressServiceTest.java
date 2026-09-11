package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.semaphore.client.UserProfileClient;
import co.edu.konradlorenz.kapp.semaphore.client.UserProfileResponse;
import co.edu.konradlorenz.kapp.semaphore.domain.CourseStatus;
import co.edu.konradlorenz.kapp.semaphore.domain.Pensum;
import co.edu.konradlorenz.kapp.semaphore.domain.PensumArea;
import co.edu.konradlorenz.kapp.semaphore.domain.PensumCourse;
import co.edu.konradlorenz.kapp.semaphore.domain.PensumStatus;
import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgress;
import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgressCourse;
import co.edu.konradlorenz.kapp.semaphore.repository.PensumRepository;
import co.edu.konradlorenz.kapp.semaphore.repository.StudentProgressRepository;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CourseProgressUpdateRequest;
import co.edu.konradlorenz.kapp.semaphore.web.dto.PensumCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ElectiveResolutionRequest;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ProgressSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for {@link StudentProgressService}: lazy creation, its concurrency
 * safety, grade cross-field validation, and the summary/eligibility arithmetic - each
 * against a small, hand-computed fixture rather than the full seeded catalogue, so the
 * expected numbers are easy to verify by eye.
 *
 * <h2>The fixture</h2>
 * Pensum "TEST" has three levels and two areas, CB (declared 7 credits) and ISA
 * (declared 9 credits), totalling the pensum's declared 16 credits:
 * <pre>
 * C1 (level 1, CB, 3cr, no prereqs)              -&gt; PASSED
 * C2 (level 2, CB, 4cr, prereq C1)                -&gt; PASSED
 * C3 (level 2, ISA, 3cr, no prereqs)              -&gt; IN_PROGRESS
 * C4 (level 3, ISA, 3cr, prereq C2)               -&gt; PENDING, eligible (C2 is PASSED)
 * C5 (level 3, ISA, 3cr, prereq C3)               -&gt; PENDING, blocked (C3 only IN_PROGRESS)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class StudentProgressServiceTest {

    private static final String USER_ID = "user-1";
    private static final String PENSUM_CODE = "TEST";

    @Mock
    private StudentProgressRepository progressRepository;
    @Mock
    private PensumRepository pensumRepository;
    @Mock
    private UserProfileClient userProfileClient;
    @Mock
    private ActivePensumResolver activePensumResolver;

    private StudentProgressService service;
    private Pensum pensum;

    @BeforeEach
    void setUp() {
        pensum = fixturePensum();
        PensumReconciler reconciler = new PensumReconciler(progressRepository);
        PrerequisiteWalker walker = new PrerequisiteWalker();
        service = new StudentProgressService(progressRepository, pensumRepository,
                userProfileClient, activePensumResolver, reconciler, walker);
    }

    // ---- lazy creation -------------------------------------------------------------

    @Test
    @DisplayName("first call materialises a fresh document from the resolved active pensum")
    void lazilyCreatesOnFirstCall() {
        when(progressRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        UserProfileResponse profile = new UserProfileResponse("u", "ROLE_STUDENT",
                new UserProfileResponse.Academic("506232730", "506", 1));
        when(userProfileClient.getMyProfile()).thenReturn(profile);
        when(activePensumResolver.resolveActiveOrThrow("506")).thenReturn(pensum);
        when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StudentProgress created = service.getOrCreate(USER_ID);

        assertThat(created.pensumCode()).isEqualTo(PENSUM_CODE);
        assertThat(created.studentCode()).isEqualTo("506232730");
        assertThat(created.courses()).hasSize(5);
        assertThat(created.courses()).allSatisfy(c -> assertThat(c.status()).isEqualTo(CourseStatus.PENDING));
    }

    @Test
    @DisplayName("a caller with no active program is reported 404, not a server error")
    void noActiveProgramIs404() {
        when(progressRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userProfileClient.getMyProfile()).thenReturn(
                new UserProfileResponse("u", "ROLE_GUEST", null));

        assertThatThrownBy(() -> service.getOrCreate(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("a duplicate-key error from the concurrent insert is resolved by re-reading, not failing")
    void concurrentFirstInsertReReadsInsteadOfFailing() {
        StudentProgress alreadyCreatedByTheOtherRequest = fixtureProgress();
        // First lookup finds nothing; after our own insert loses the race, the second
        // lookup (inside the catch block) finds what the winning request created.
        when(progressRepository.findByUserId(USER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(alreadyCreatedByTheOtherRequest));
        UserProfileResponse profile = new UserProfileResponse("u", "ROLE_STUDENT",
                new UserProfileResponse.Academic("506232730", "506", 1));
        when(userProfileClient.getMyProfile()).thenReturn(profile);
        when(activePensumResolver.resolveActiveOrThrow("506")).thenReturn(pensum);
        when(progressRepository.save(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));

        StudentProgress result = service.getOrCreate(USER_ID);

        assertThat(result).isSameAs(alreadyCreatedByTheOtherRequest);
        verify(progressRepository, times(2)).findByUserId(USER_ID);
    }

    // ---- grade cross-field validation ------------------------------------------------

    @Test
    @DisplayName("PASSED without a grade is rejected")
    void passedWithoutGradeIsRejected() {
        // Cross-field validation runs before any repository lookup, so nothing is
        // stubbed here - the point is that this never touches the database.
        CourseProgressUpdateRequest request = new CourseProgressUpdateRequest(
                CourseStatus.PASSED, null, "20261");

        assertThatThrownBy(() -> service.updateCourseStatus(USER_ID, "C1", request))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getDetails())
                        .anySatisfy(issue -> assertThat(issue.field()).isEqualTo("grade")));
    }

    @Test
    @DisplayName("FAILED without a grade is rejected")
    void failedWithoutGradeIsRejected() {
        CourseProgressUpdateRequest request = new CourseProgressUpdateRequest(
                CourseStatus.FAILED, null, "20261");

        assertThatThrownBy(() -> service.updateCourseStatus(USER_ID, "C1", request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("PENDING with a grade is rejected - a course not yet taken has no mark")
    void pendingWithGradeIsRejected() {
        CourseProgressUpdateRequest request = new CourseProgressUpdateRequest(
                CourseStatus.PENDING, 30, null);

        assertThatThrownBy(() -> service.updateCourseStatus(USER_ID, "C4", request))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getDetails())
                        .anySatisfy(issue -> assertThat(issue.field()).isEqualTo("grade")));
    }

    @Test
    @DisplayName("PASSED with a grade is accepted and persisted")
    void passedWithGradeIsAccepted() {
        stubExistingProgress();
        when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CourseProgressUpdateRequest request = new CourseProgressUpdateRequest(
                CourseStatus.PASSED, 42, "20261");

        var updated = service.updateCourseStatus(USER_ID, "C4", request);

        assertThat(updated.status()).isEqualTo(CourseStatus.PASSED);
        assertThat(updated.grade()).isEqualTo(42);
    }

    @Test
    @DisplayName("an unknown course code on the caller's pinned pensum is 404")
    void unknownCourseCodeIs404() {
        stubExistingProgress();
        CourseProgressUpdateRequest request = new CourseProgressUpdateRequest(
                CourseStatus.IN_PROGRESS, null, "20261");

        assertThatThrownBy(() -> service.updateCourseStatus(USER_ID, "NO-SUCH-COURSE", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- summary arithmetic -----------------------------------------------------------

    @Test
    @DisplayName("summary arithmetic matches the hand-computed fixture exactly")
    void summaryArithmeticMatchesFixture() {
        stubExistingProgress();

        ProgressSummaryDto summary = service.getSummary(USER_ID);

        assertThat(summary.creditsPassed()).isEqualTo(7);       // C1(3) + C2(4)
        assertThat(summary.creditsInProgress()).isEqualTo(3);   // C3(3)
        assertThat(summary.totalCredits()).isEqualTo(16);
        assertThat(summary.creditsRemaining()).isEqualTo(6);    // 16 - 7 - 3
        assertThat(summary.percentComplete()).isEqualTo(43.8);  // 7/16*100, one decimal
        assertThat(summary.currentLevel()).isEqualTo(2);        // lowest unfinished: C3 at level 2

        assertThat(summary.byArea()).extracting("area", "creditsPassed", "creditsTotal")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("CB", 7, 7),
                        org.assertj.core.groups.Tuple.tuple("ISA", 0, 9));
    }

    // ---- eligibility --------------------------------------------------------------

    @Test
    @DisplayName("eligible lists only PENDING items whose prerequisites are all PASSED")
    void eligibleRespectsPrerequisitesAndOwnStatus() {
        stubExistingProgress();

        List<PensumCourseDto> eligible = service.getEligible(USER_ID);

        assertThat(eligible).extracting(PensumCourseDto::code).containsExactly("C4");
    }

    @Test
    @DisplayName("an IN_PROGRESS prerequisite does not unlock what depends on it")
    void inProgressPrerequisiteBlocksEligibility() {
        stubExistingProgress();

        List<PensumCourseDto> eligible = service.getEligible(USER_ID);

        assertThat(eligible).extracting(PensumCourseDto::code).doesNotContain("C5");
    }

    // ---- elective resolution -------------------------------------------------------

    @Test
    @DisplayName("resolving an item that is not an elective slot is rejected")
    void resolvingANonElectiveItemIsRejected() {
        stubElectiveFixture();
        var request = new ElectiveResolutionRequest(
                "EXTERNAL-1", "External Course");

        assertThatThrownBy(() -> service.resolveElective(USER_ID, "F1", request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("resolving to a course already accounted for elsewhere on the semaforo is rejected")
    void resolvingToAnAlreadyUsedCourseIsRejected() {
        stubElectiveFixture();
        // F1 is already a fixed item of this same pensum under code "F1".
        var request = new ElectiveResolutionRequest(
                "F1", "Duplicate of F1");

        assertThatThrownBy(() -> service.resolveElective(USER_ID, "ELEC1", request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("resolving an elective slot records the real course without touching its status")
    void resolvingAnElectiveSlotRecordsTheCourse() {
        stubElectiveFixture();
        when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var request = new ElectiveResolutionRequest(
                "EXTERNAL-1", "External Course");

        var updated = service.resolveElective(USER_ID, "ELEC1", request);

        assertThat(updated.resolvedCode()).isEqualTo("EXTERNAL-1");
        assertThat(updated.resolvedName()).isEqualTo("External Course");
        assertThat(updated.status()).isEqualTo(CourseStatus.PENDING);
        assertThat(updated.code()).isNull();
    }

    @Test
    @DisplayName("clearing a resolved elective slot returns it to empty")
    void clearingAResolvedElectiveSlotEmptiesIt() {
        stubElectiveFixture(true);
        when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.clearElective(USER_ID, "ELEC1");

        var saved = ArgumentCaptor.forClass(StudentProgress.class);
        verify(progressRepository).save(saved.capture());
        assertThat(saved.getValue().findByPensumItemCode("ELEC1"))
                .hasValueSatisfying(entry -> {
                    assertThat(entry.resolvedCode()).isNull();
                    assertThat(entry.resolvedName()).isNull();
                });
    }

    @Test
    @DisplayName("clearing an already-empty elective slot is a no-op, not an error")
    void clearingAnAlreadyEmptyElectiveSlotIsANoOp() {
        stubElectiveFixture(false);

        service.clearElective(USER_ID, "ELEC1");

        verify(progressRepository, never()).save(any());
    }

    // ---- fixtures -------------------------------------------------------------------

    private void stubExistingProgress() {
        when(progressRepository.findByUserId(USER_ID)).thenReturn(Optional.of(fixtureProgress()));
        when(pensumRepository.findById(PENSUM_CODE)).thenReturn(Optional.of(pensum));
    }

    /**
     * A second, deliberately separate fixture for elective-resolution tests: one fixed
     * course "F1" and one elective slot "ELEC1", independent of the C1..C5 fixture above
     * so those arithmetic/eligibility tests are never at risk of an unrelated edit here.
     */
    private void stubElectiveFixture() {
        stubElectiveFixture(false);
    }

    private void stubElectiveFixture(boolean electiveAlreadyResolved) {
        PensumCourse fixed = new PensumCourse("F1", "F1", "Fixed Course", 1, 3, 4, "CB",
                false, List.of(), null);
        PensumCourse elective = new PensumCourse(null, "ELEC1", "Elective Slot", 1, 3, 3,
                "CB", true, List.of(), null);
        Pensum electivePensum = new Pensum(PENSUM_CODE, "506", "Ingenieria de Sistemas",
                "Facultad", "Reforma test", PensumStatus.ACTIVE, 6, 7, 1,
                List.of(new PensumArea("CB", "Ciencias Basicas", "#539392", 6, 7)),
                List.of(fixed, elective));

        StudentProgressCourse fixedEntry = new StudentProgressCourse(
                "F1", "F1", CourseStatus.PENDING, null, null, null, null);
        StudentProgressCourse electiveEntry = electiveAlreadyResolved
                ? new StudentProgressCourse(null, "ELEC1", CourseStatus.PENDING, null, null,
                        "PRIOR-CODE", "Prior Course")
                : new StudentProgressCourse(null, "ELEC1", CourseStatus.PENDING, null, null, null, null);
        StudentProgress progress = new StudentProgress("doc-elec", USER_ID, "506232730", "506",
                PENSUM_CODE, 1, List.of(fixedEntry, electiveEntry), Instant.now());

        when(progressRepository.findByUserId(USER_ID)).thenReturn(Optional.of(progress));
        when(pensumRepository.findById(PENSUM_CODE)).thenReturn(Optional.of(electivePensum));
    }

    private static Pensum fixturePensum() {
        List<PensumCourse> courses = List.of(
                item("C1", 1, "CB", 3, List.of()),
                item("C2", 2, "CB", 4, List.of("C1")),
                item("C3", 2, "ISA", 3, List.of()),
                item("C4", 3, "ISA", 3, List.of("C2")),
                item("C5", 3, "ISA", 3, List.of("C3")));
        List<PensumArea> areas = List.of(
                new PensumArea("CB", "Ciencias Basicas", "#539392", 7, 7),
                new PensumArea("ISA", "Ing. de Sistemas Aplicada", "#C9D329", 9, 9));
        return new Pensum(PENSUM_CODE, "506", "Ingenieria de Sistemas", "Facultad",
                "Reforma test", PensumStatus.ACTIVE, 16, 16, 3, areas, courses);
    }

    private static PensumCourse item(String code, int level, String area, int credits,
                                          List<String> prerequisites) {
        return new PensumCourse(code, code, "Course " + code, level, credits, credits, area,
                false, prerequisites, null);
    }

    private static StudentProgress fixtureProgress() {
        List<StudentProgressCourse> courses = List.of(
                entry("C1", CourseStatus.PASSED, 40),
                entry("C2", CourseStatus.PASSED, 35),
                entry("C3", CourseStatus.IN_PROGRESS, null),
                entry("C4", CourseStatus.PENDING, null),
                entry("C5", CourseStatus.PENDING, null));
        return new StudentProgress("doc-1", USER_ID, "506232730", "506", PENSUM_CODE, 2,
                courses, Instant.now());
    }

    private static StudentProgressCourse entry(String code, CourseStatus status, Integer grade) {
        return new StudentProgressCourse(code, code, status, grade != null ? "20221" : null,
                grade, null, null);
    }
}
