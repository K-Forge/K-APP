package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.semaphore.client.UserProfileClient;
import co.edu.konradlorenz.kapp.semaphore.client.UserProfileResponse;
import co.edu.konradlorenz.kapp.semaphore.domain.CourseStatus;
import co.edu.konradlorenz.kapp.semaphore.domain.Curriculum;
import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumCourse;
import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgress;
import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgressCourse;
import co.edu.konradlorenz.kapp.semaphore.repository.CurriculumRepository;
import co.edu.konradlorenz.kapp.semaphore.repository.StudentProgressRepository;
import co.edu.konradlorenz.kapp.semaphore.web.dto.AreaProgressDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CourseProgressUpdateRequest;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ElectiveResolutionRequest;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ProgressSummaryDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.StudentProgressCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.StudentProgressDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.StudentProgressWithReconciliationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The student's own semaforo: lazy creation, reconciliation, course status, elective
 * resolution, the derived summary and eligibility.
 *
 * <p>Every {@code /me/**} operation starts from {@link #loadReconciled(String)}, which
 * lazily creates the document if needed and then reconciles it against its pinned
 * curriculum. Routing every entry point through the same method is what guarantees a
 * pensum item added by an admin after this document existed is available to
 * {@code PUT /courses/{code}} and {@code POST /electives/{pensumItemCode}} immediately,
 * not only after the student happens to call {@code GET /me} first.
 */
@Service
public class StudentProgressService {

    private static final Logger log = LoggerFactory.getLogger(StudentProgressService.class);

    private final StudentProgressRepository progressRepository;
    private final CurriculumRepository curriculumRepository;
    private final UserProfileClient userProfileClient;
    private final ActivePensumResolver activePensumResolver;
    private final CurriculumReconciler reconciler;
    private final PrerequisiteWalker prerequisiteWalker;

    public StudentProgressService(StudentProgressRepository progressRepository,
                                   CurriculumRepository curriculumRepository,
                                   UserProfileClient userProfileClient,
                                   ActivePensumResolver activePensumResolver,
                                   CurriculumReconciler reconciler,
                                   PrerequisiteWalker prerequisiteWalker) {
        this.progressRepository = progressRepository;
        this.curriculumRepository = curriculumRepository;
        this.userProfileClient = userProfileClient;
        this.activePensumResolver = activePensumResolver;
        this.reconciler = reconciler;
        this.prerequisiteWalker = prerequisiteWalker;
    }

    public StudentProgressWithReconciliationDto getMineWithReconciliation(String userId) {
        StudentProgress progress = getOrCreate(userId);
        Curriculum curriculum = requireCurriculum(progress.pensumCode());
        CurriculumReconciler.Result result = reconciler.reconcile(progress, curriculum);
        return StudentProgressWithReconciliationDto.of(result.progress(), result.reconciliation());
    }

    public StudentProgressDto getForAdmin(String userId) {
        StudentProgress progress = progressRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student progress", userId));
        return StudentProgressDto.from(progress);
    }

    public ProgressSummaryDto getSummary(String userId) {
        StudentProgress progress = loadReconciled(userId);
        Curriculum curriculum = requireCurriculum(progress.pensumCode());
        Map<String, CurriculumCourse> itemsByPensumCode = curriculum.byPensumItemCode();

        int creditsPassed = 0;
        int creditsInProgress = 0;
        int minUnfinishedLevel = Integer.MAX_VALUE;
        Map<String, Integer> passedCreditsByArea = new LinkedHashMap<>();
        curriculum.areaCodes().forEach(code -> passedCreditsByArea.put(code, 0));

        for (StudentProgressCourse entry : progress.courses()) {
            CurriculumCourse item = itemsByPensumCode.get(entry.pensumItemCode());
            // A removed item (see CurriculumReconciler) is kept so its grade is never
            // lost, but it no longer belongs to the plan and does not count here.
            if (item == null) {
                continue;
            }
            if (entry.status() == CourseStatus.PASSED) {
                creditsPassed += item.credits();
                passedCreditsByArea.merge(item.area(), item.credits(), Integer::sum);
            } else if (entry.status() == CourseStatus.IN_PROGRESS) {
                creditsInProgress += item.credits();
            }
            if (entry.status() != CourseStatus.PASSED) {
                minUnfinishedLevel = Math.min(minUnfinishedLevel, item.level());
            }
        }

        int totalCredits = curriculum.totalCredits();
        int creditsRemaining = totalCredits - creditsPassed - creditsInProgress;
        double percentComplete = totalCredits == 0 ? 0.0 : roundToOneDecimal(creditsPassed * 100.0 / totalCredits);
        List<AreaProgressDto> byArea = curriculum.areas().stream()
                .map(area -> new AreaProgressDto(area.code(),
                        passedCreditsByArea.getOrDefault(area.code(), 0), area.credits()))
                .toList();
        // Nothing left unfinished: every item is PASSED, so there is no "current" level
        // to derive. The last level of the plan is the least surprising answer.
        int currentLevel = minUnfinishedLevel == Integer.MAX_VALUE ? curriculum.levels() : minUnfinishedLevel;

        return new ProgressSummaryDto(creditsPassed, creditsInProgress, creditsRemaining,
                totalCredits, percentComplete, byArea, currentLevel);
    }

    public List<CurriculumCourseDto> getEligible(String userId) {
        StudentProgress progress = loadReconciled(userId);
        Curriculum curriculum = requireCurriculum(progress.pensumCode());
        Map<String, StudentProgressCourse> byCourseCode = progress.byCourseCode();

        return curriculum.coursesInDisplayOrder().stream()
                .filter(item -> isPending(progress, item))
                .filter(item -> prerequisiteWalker.allPrerequisitesPassed(item, byCourseCode))
                .map(CurriculumCourseDto::from)
                .toList();
    }

    private boolean isPending(StudentProgress progress, CurriculumCourse item) {
        return progress.findByPensumItemCode(item.pensumItemCode())
                .map(entry -> entry.status() == CourseStatus.PENDING)
                .orElse(false);
    }

    /**
     * @throws BusinessRuleException     (400) for the settled/grade cross-field rules
     * @throws ResourceNotFoundException (404) if {@code code} names no item of the
     *                                    caller's pinned pensum
     */
    public StudentProgressCourseDto updateCourseStatus(String userId, String code,
                                                        CourseProgressUpdateRequest request) {
        validateStatusAndGrade(request);

        StudentProgress progress = loadReconciled(userId);
        StudentProgressCourse existing = progress.findByAddressableCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Pensum item", code));

        StudentProgressCourse updated = existing.withStatus(request.status(), request.grade(), request.period());
        StudentProgress saved = progressRepository.save(progress.withCourse(updated));
        return StudentProgressCourseDto.from(
                saved.findByAddressableCode(code).orElseThrow());
    }

    private void validateStatusAndGrade(CourseProgressUpdateRequest request) {
        if (request.status().isSettled() && request.grade() == null) {
            throw new BusinessRuleException("A settled course must carry a grade", List.of(
                    new ApiError.FieldIssue("grade", "is required when status is PASSED or FAILED")));
        }
        if (request.status() == CourseStatus.PENDING && request.grade() != null) {
            throw new BusinessRuleException("A course that has not been taken has no grade", List.of(
                    new ApiError.FieldIssue("grade", "must not be supplied when status is PENDING")));
        }
    }

    /**
     * @throws BusinessRuleException     (400) if {@code pensumItemCode} names an item
     *                                    that is not an elective slot, or if
     *                                    {@code resolvedCode} is already accounted for
     *                                    elsewhere on the semaforo
     * @throws ResourceNotFoundException (404) if the caller's pinned pensum has no such
     *                                    item
     */
    public StudentProgressCourseDto resolveElective(String userId, String pensumItemCode,
                                                     ElectiveResolutionRequest request) {
        StudentProgress progress = loadReconciled(userId);
        Curriculum curriculum = requireCurriculum(progress.pensumCode());
        requireElectiveSlot(curriculum, pensumItemCode);

        boolean alreadyUsedElsewhere = progress.courses().stream()
                .filter(entry -> !entry.pensumItemCode().equals(pensumItemCode))
                .anyMatch(entry -> request.resolvedCode().equals(entry.code())
                        || request.resolvedCode().equals(entry.resolvedCode()));
        if (alreadyUsedElsewhere) {
            throw new BusinessRuleException("Course is already accounted for elsewhere on the semaforo", List.of(
                    new ApiError.FieldIssue("resolvedCode",
                            "'%s' is already recorded on another item".formatted(request.resolvedCode()))));
        }

        StudentProgressCourse existing = progress.findByPensumItemCode(pensumItemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Pensum item", pensumItemCode));
        StudentProgressCourse updated = existing.withResolution(request.resolvedCode(), request.resolvedName());
        StudentProgress saved = progressRepository.save(progress.withCourse(updated));
        return StudentProgressCourseDto.from(saved.findByPensumItemCode(pensumItemCode).orElseThrow());
    }

    /**
     * Idempotent: clearing an already-empty slot is a no-op that still succeeds.
     *
     * @throws BusinessRuleException     (400) if {@code pensumItemCode} names an item
     *                                    that is not an elective slot
     * @throws ResourceNotFoundException (404) if the caller's pinned pensum has no such
     *                                    item
     */
    public void clearElective(String userId, String pensumItemCode) {
        StudentProgress progress = loadReconciled(userId);
        Curriculum curriculum = requireCurriculum(progress.pensumCode());
        requireElectiveSlot(curriculum, pensumItemCode);

        StudentProgressCourse existing = progress.findByPensumItemCode(pensumItemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Pensum item", pensumItemCode));
        if (existing.resolvedCode() == null && existing.resolvedName() == null) {
            return;
        }
        progressRepository.save(progress.withCourse(existing.withResolution(null, null)));
    }

    private void requireElectiveSlot(Curriculum curriculum, String pensumItemCode) {
        CurriculumCourse item = curriculum.findByPensumItemCode(pensumItemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Pensum item", pensumItemCode));
        if (!item.electiveSlot()) {
            throw new BusinessRuleException("Pensum item is not an elective slot", List.of(
                    new ApiError.FieldIssue("pensumItemCode",
                            "'%s' is a fixed course, not an elective slot".formatted(pensumItemCode))));
        }
    }

    private StudentProgress loadReconciled(String userId) {
        StudentProgress progress = getOrCreate(userId);
        Curriculum curriculum = requireCurriculum(progress.pensumCode());
        return reconciler.reconcile(progress, curriculum).progress();
    }

    /**
     * Lazily creates the caller's semaforo. Never returns {@code 404} for a student who
     * has an active program - only when the caller has no program, or the program has
     * no {@code ACTIVE} pensum to pin.
     *
     * <h2>Concurrency</h2>
     * Two requests racing to create the same student's first document both pass the
     * {@code findByUserId} check with nothing there, and both attempt to insert. The
     * unique index on {@code {userId, pensumCode}} (see {@code V001_SemaphoreIndexes})
     * lets exactly one of those inserts succeed; the other fails with
     * {@link DuplicateKeyException} at the database, and this method re-reads instead of
     * treating that as an error. There is deliberately no pre-check-then-insert: a
     * pre-check only narrows the race window, it does not close it, and the unique index
     * is the actual source of truth for "does this document exist".
     */
    public StudentProgress getOrCreate(String userId) {
        return progressRepository.findByUserId(userId)
                .orElseGet(() -> createFor(userId));
    }

    private StudentProgress createFor(String userId) {
        UserProfileResponse profile = userProfileClient.getMyProfile();
        UserProfileResponse.Academic academic = profile.academic();
        if (academic == null || academic.programCode() == null) {
            throw new ResourceNotFoundException("Active program for user", userId);
        }

        Curriculum curriculum = activePensumResolver.resolveActiveOrThrow(academic.programCode());
        int currentLevel = academic.currentLevel() != null ? academic.currentLevel() : 1;
        StudentProgress fresh = StudentProgress.materialise(
                userId, academic.studentCode(), curriculum, currentLevel);

        try {
            return progressRepository.save(fresh);
        } catch (DuplicateKeyException e) {
            log.debug("Concurrent first read for user {}; re-reading instead of failing", userId);
            return progressRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Duplicate key on studentProgress but no document found for user " + userId, e));
        }
    }

    private Curriculum requireCurriculum(String pensumCode) {
        return curriculumRepository.findById(pensumCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Pinned curriculum missing: " + pensumCode));
    }

    private static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
