package co.edu.konradlorenz.kapp.semaphore.domain;

/**
 * Stored status of one item on a student's semaforo.
 *
 * <p>There are five colours in the grid but only four values here. <em>Blocked</em> is
 * NOT stored: it is derived as a {@link #PENDING} item whose prerequisites are not all
 * {@link #PASSED}, and it is exactly the complement of
 * {@code GET /api/semaphore/me/eligible} within the pending set. Storing it would create
 * a second source of truth that goes stale the moment a grade is recorded.
 */
public enum CourseStatus {
    PASSED,
    IN_PROGRESS,
    PENDING,
    FAILED;

    /** @return true when the status is one a grade must accompany */
    public boolean isSettled() {
        return this == PASSED || this == FAILED;
    }
}
