package co.edu.konradlorenz.kapp.semaphore.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The real course an elective slot is being resolved to.
 *
 * <p>{@code resolvedCode} does not have to belong to the student's own pensum: electives
 * are frequently taken from other programs, which is the whole point of a slot.
 */
public record ElectiveResolutionRequest(
        @NotBlank @Size(max = 20) String resolvedCode,
        @NotBlank @Size(max = 120) String resolvedName
) {
}
