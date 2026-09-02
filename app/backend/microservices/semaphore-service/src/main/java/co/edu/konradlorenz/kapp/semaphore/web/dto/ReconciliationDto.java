package co.edu.konradlorenz.kapp.semaphore.web.dto;

import java.util.List;

/**
 * How a stored progress document compares with the curriculum it pins.
 *
 * <p>Identifiers are course codes, except for elective slots, which have none and are
 * reported by {@code pensumItemCode}.
 *
 * @param addedCourses   items the curriculum has and the document did not; they have been
 *                       materialised as PENDING
 * @param removedCourses entries whose pensum item no longer exists in the curriculum; they
 *                       are KEPT in {@code courses} so no grade is destroyed, and excluded
 *                       from the summary
 * @param inSync         true exactly when both arrays are empty
 */
public record ReconciliationDto(
        List<String> addedCourses,
        List<String> removedCourses,
        boolean inSync
) {

    public static ReconciliationDto of(List<String> added, List<String> removed) {
        return new ReconciliationDto(List.copyOf(added), List.copyOf(removed),
                added.isEmpty() && removed.isEmpty());
    }

    public static ReconciliationDto inSync() {
        return new ReconciliationDto(List.of(), List.of(), true);
    }
}
