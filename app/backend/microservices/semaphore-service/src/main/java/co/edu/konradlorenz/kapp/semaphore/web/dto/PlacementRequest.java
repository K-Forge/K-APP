package co.edu.konradlorenz.kapp.semaphore.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Where the student wants a course to sit.
 *
 * <p>1 to 12 matches the range a curriculum may declare. There is deliberately no check that
 * the level is within <em>this</em> pensum's {@code levels}: a student delaying a course past
 * the nominal end of their programme is a real plan, and the common one.
 */
public record PlacementRequest(@NotNull @Min(1) @Max(12) Integer plannedLevel) {
}
