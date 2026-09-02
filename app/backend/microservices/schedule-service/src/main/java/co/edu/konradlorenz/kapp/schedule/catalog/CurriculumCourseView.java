package co.edu.konradlorenz.kapp.schedule.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The slice of {@code semaphore-service}'s {@code CurriculumCourse} this service actually
 * needs, from {@code GET /api/catalog/curricula/{pensumCode}/courses}
 * ({@code docs/api/semaphore.openapi.yaml}).
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} is required, not decorative: this
 * service's default {@code ObjectMapper} fails on unknown properties (nothing here
 * disables that), and the real schema carries several fields - {@code weeklyHours},
 * {@code totalHours}, {@code area}, {@code isElectiveSlot}, {@code prerequisites} - this
 * client has no use for. Without it, semaphore-service adding a field would break every
 * catalogue lookup here.
 *
 * <p>An elective slot has {@code code == null}; matching a hand-entered enrollment is
 * therefore done by {@code pensumItemCode}, which is never null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CurriculumCourseView(
        String code,
        String pensumItemCode,
        String name,
        Integer level,
        Integer credits
) {
}
