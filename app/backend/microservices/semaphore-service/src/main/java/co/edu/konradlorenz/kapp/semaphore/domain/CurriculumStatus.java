package co.edu.konradlorenz.kapp.semaphore.domain;

/**
 * Lifecycle state of a pensum.
 *
 * <p>Only an {@link #ACTIVE} plan is pinned to a student by {@code GET /api/semaphore/me}.
 * A program may hold many curricula at once - students who enrolled under an earlier
 * reform keep theirs - but at most one of them is active.
 */
public enum CurriculumStatus {
    ACTIVE,
    DRAFT,
    OBSOLETE
}
