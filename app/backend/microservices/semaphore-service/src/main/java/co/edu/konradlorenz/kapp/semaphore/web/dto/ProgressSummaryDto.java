package co.edu.konradlorenz.kapp.semaphore.web.dto;

import java.util.List;

/**
 * Credit totals derived from a semaforo.
 *
 * <p><strong>Never stored.</strong> Recomputed on every call from the progress document
 * and the pinned pensum, so it cannot drift from the grid the student is looking at.
 * A stored summary would be a second source of truth that goes stale the moment a grade
 * is recorded and nobody notices for a semester.
 *
 * @param creditsRemaining {@code totalCredits - creditsPassed - creditsInProgress}, so
 *                         PENDING and FAILED both count as remaining
 * @param percentComplete  {@code creditsPassed / totalCredits * 100}, one decimal
 * @param currentLevel     derived: the lowest level that still holds an unfinished item
 */
public record ProgressSummaryDto(
        int creditsPassed,
        int creditsInProgress,
        int creditsRemaining,
        int totalCredits,
        double percentComplete,
        List<AreaProgressDto> byArea,
        int currentLevel
) {
}
