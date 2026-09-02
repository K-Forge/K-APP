package co.edu.konradlorenz.kapp.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Guest signup payload. Any e-mail domain, no invitation code, always {@code ROLE_GUEST}.
 *
 * <p>A {@code @konradlorenz.edu.co} address is accepted here too, and still produces a
 * guest account with no academic record - the endpoint decides the role, not the domain.
 */
public record GuestRegistrationRequest(

        @NotBlank
        @Email
        @Size(max = 100)
        @Schema(example = "maria.rodriguez@gmail.com")
        String email,

        @NotBlank
        @Size(min = 10, max = 72, message = "must be between 10 and 72 characters")
        @Schema(example = "VisitanteSegura!26")
        String password,

        @NotBlank
        @Size(max = 50)
        @Schema(example = "Maria Fernanda")
        String firstName,

        @NotBlank
        @Size(max = 50)
        @Schema(example = "Rodriguez Pena")
        String lastName
) {
}
