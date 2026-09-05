package co.edu.konradlorenz.kapp.semaphore.web.dto;

import jakarta.validation.constraints.Size;

/**
 * A partial update of a plan. Both fields optional, but not both absent.
 *
 * <p>Boxed {@code Boolean} rather than {@code boolean}: the difference between "make this the
 * primary" and "do not touch primary" is exactly the difference between {@code true} and
 * {@code null}, and a primitive would collapse the second into {@code false} and silently
 * demote the plan on every rename.
 *
 * @param primary only {@code true} is accepted. Demoting without promoting another would leave
 *                the student with no primary at all, and something has to be the plan the app
 *                opens on.
 */
public record AcademicPlanUpdate(
        @Size(max = 60) String name,
        Boolean primary
) {

    public boolean isEmpty() {
        return name == null && primary == null;
    }
}
