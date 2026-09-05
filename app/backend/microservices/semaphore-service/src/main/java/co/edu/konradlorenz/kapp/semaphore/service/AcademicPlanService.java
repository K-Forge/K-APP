package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.common.error.ConflictException;
import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.semaphore.domain.AcademicPlan;
import co.edu.konradlorenz.kapp.semaphore.domain.Curriculum;
import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumCourse;
import co.edu.konradlorenz.kapp.semaphore.repository.AcademicPlanRepository;
import co.edu.konradlorenz.kapp.semaphore.repository.CurriculumRepository;
import co.edu.konradlorenz.kapp.semaphore.web.dto.AcademicPlanRequest;
import co.edu.konradlorenz.kapp.semaphore.web.dto.AcademicPlanUpdate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The student's own arrangements of their pensum.
 *
 * <p>Deliberately separate from {@link StudentProgressService}. Progress records what has been
 * passed; a plan records what the student intends to take and when. Keeping them apart is what
 * guarantees the rule that matters most here: <strong>moving a course never changes
 * eligibility</strong>. Nothing in this class touches a progress document, and
 * {@code getEligible} never reads a plan, so a student cannot unlock a course by dragging it
 * into an earlier semester.
 *
 * <p>Every method takes {@code userId} from the validated token. A plan belonging to somebody
 * else is reported as {@code 404}, not {@code 403}: a 403 would confirm that the identifier
 * exists, which is more than a caller who does not own it should learn.
 */
@Service
public class AcademicPlanService {

    /**
     * Enough for "what if I take Cálculo in the summer" several times over, and short of an
     * unbounded collection accumulating under one account.
     */
    static final int MAX_PLANS_PER_PENSUM = 10;

    private final AcademicPlanRepository plans;
    private final CurriculumRepository curricula;

    public AcademicPlanService(AcademicPlanRepository plans, CurriculumRepository curricula) {
        this.plans = plans;
        this.curricula = curricula;
    }

    public List<AcademicPlan> list(String userId) {
        return plans.findByUserIdOrderByPrimaryDescCreatedAtAsc(userId);
    }

    public AcademicPlan get(String userId, String planId) {
        return requireOwn(userId, planId);
    }

    /**
     * @throws ResourceNotFoundException (404) if the pensum does not exist. A plan over a
     *                                    curriculum nobody published cannot be drawn
     * @throws ConflictException         (409) at {@link #MAX_PLANS_PER_PENSUM}
     */
    public AcademicPlan create(String userId, AcademicPlanRequest request) {
        Curriculum curriculum = curricula.findById(request.pensumCode())
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum", request.pensumCode()));

        long existing = plans.countByUserIdAndPensumCode(userId, curriculum.pensumCode());
        if (existing >= MAX_PLANS_PER_PENSUM) {
            throw new ConflictException(
                    "You already have %d plans for pensum %s, which is the maximum"
                            .formatted(MAX_PLANS_PER_PENSUM, curriculum.pensumCode()),
                    List.of(new ApiError.FieldIssue("pensumCode", curriculum.pensumCode())));
        }

        // The first plan for a pensum is the primary one. Anything else would leave a student
        // who created exactly one plan with no plan for the app to open on.
        boolean primary = existing == 0;
        return plans.save(AcademicPlan.create(userId, request.name(), curriculum.pensumCode(), primary));
    }

    /**
     * @throws BusinessRuleException (400) if the body carries nothing to change, or asks to
     *                               demote a plan without promoting another
     */
    public AcademicPlan update(String userId, String planId, AcademicPlanUpdate update) {
        if (update.isEmpty()) {
            throw new BusinessRuleException("Nothing to update",
                    List.of(new ApiError.FieldIssue("name", "supply a name, primary, or both")));
        }
        if (Boolean.FALSE.equals(update.primary())) {
            throw new BusinessRuleException(
                    "A plan cannot be demoted directly; promote another one instead",
                    List.of(new ApiError.FieldIssue("primary",
                            "only true is accepted - promoting a plan demotes the current primary")));
        }

        AcademicPlan plan = requireOwn(userId, planId);
        if (update.name() != null) {
            plan = plan.renamedTo(update.name());
        }
        if (Boolean.TRUE.equals(update.primary()) && !plan.primary()) {
            demoteCurrentPrimary(userId, plan.pensumCode(), planId);
            plan = plan.withPrimary(true);
        }
        return plans.save(plan);
    }

    /**
     * Deleting the primary promotes the next plan by creation date, so a student who deletes
     * the one the app opens on is not left with a set of plans and no primary among them.
     */
    public void delete(String userId, String planId) {
        AcademicPlan plan = requireOwn(userId, planId);
        plans.deleteById(planId);

        if (plan.primary()) {
            plans.findByUserIdAndPensumCodeOrderByCreatedAtAsc(userId, plan.pensumCode()).stream()
                    .findFirst()
                    .ifPresent(next -> plans.save(next.withPrimary(true)));
        }
    }

    /**
     * Pins a course to a level. Idempotent.
     *
     * @throws BusinessRuleException (400) if the identifier is not an item of the plan's
     *                               pensum. Accepting an unknown code would let a plan carry
     *                               placements for courses the grid has no square for, which
     *                               the client would silently drop while the student believed
     *                               the move was saved
     */
    public AcademicPlan place(String userId, String planId, String code, int plannedLevel) {
        AcademicPlan plan = requireOwn(userId, planId);
        Curriculum curriculum = curricula.findById(plan.pensumCode())
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum", plan.pensumCode()));

        if (!addressableCodes(curriculum).contains(code)) {
            throw new BusinessRuleException(
                    "%s is not an item of pensum %s".formatted(code, plan.pensumCode()),
                    List.of(new ApiError.FieldIssue("code", "unknown in this pensum")));
        }
        return plans.save(plan.withPlacement(code, plannedLevel));
    }

    /**
     * @throws ResourceNotFoundException (404) if the plan has no placement for that course. It
     *                                    was never moved, so there is nothing to undo, and
     *                                    answering 204 would tell a client it had reset
     *                                    something it had not
     */
    public AcademicPlan reset(String userId, String planId, String code) {
        AcademicPlan plan = requireOwn(userId, planId);
        if (plan.placementFor(code).isEmpty()) {
            throw new ResourceNotFoundException("Placement", code);
        }
        return plans.save(plan.withoutPlacement(code));
    }

    private static Set<String> addressableCodes(Curriculum curriculum) {
        return curriculum.coursesInDisplayOrder().stream()
                .map(CurriculumCourse::addressableCode)
                .collect(Collectors.toSet());
    }

    private AcademicPlan requireOwn(String userId, String planId) {
        return plans.findById(planId)
                .filter(p -> p.userId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Academic plan", planId));
    }

    /**
     * Demotes the current primary before the new one is written.
     *
     * <p>The order matters: the partial unique index forbids two primaries for one student and
     * pensum, so promoting first would collide with the plan being replaced. Demoting first
     * leaves a moment with no primary, which is the safe direction - a read landing in that
     * window sees the plans unordered, not an error.
     */
    private void demoteCurrentPrimary(String userId, String pensumCode, String exceptPlanId) {
        plans.findByUserIdAndPensumCodeAndPrimaryIsTrue(userId, pensumCode)
                .filter(current -> !current.id().equals(exceptPlanId))
                .ifPresent(current -> plans.save(current.withPrimary(false)));
    }
}
