package co.edu.konradlorenz.kapp.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Institutional signup payload, mirroring {@code RegistrationRequest} in
 * {@code docs/api/auth.openapi.yaml}.
 *
 * <p>The institutional domain is checked in the service rather than with a
 * {@code @Pattern} here, even though the spec expresses it as one. The domain is
 * configurable through {@code KAPP_ALLOWED_EMAIL_DOMAINS}, and a hard-coded pattern would
 * quietly win over the setting - rejecting an address the deployment was configured to
 * accept, with a validation message that pointed at neither. Both paths answer 400 either
 * way, so the contract is unaffected.
 *
 * @param studentCode  required for a student intake, absent for a staff invitation. The
 *                     invitation code decides which of the two this is, so the requirement
 *                     cannot be expressed as an annotation.
 */
public record RegistrationRequest(

        @NotBlank
        @Email
        @Size(max = 100)
        @Schema(example = "pepito.perez@konradlorenz.edu.co")
        String email,

        @NotBlank
        @Size(min = 10, max = 72, message = "must be between 10 and 72 characters")
        @Schema(description = "Plain text, at least 10 characters. 72 is the BCrypt input limit.",
                example = "ExamplePassword123")
        String password,

        @NotBlank
        @Size(max = 50)
        @Schema(example = "Pepito")
        String firstName,

        @NotBlank
        @Size(max = 50)
        @Schema(example = "Perez Gomez")
        String lastName,

        @NotBlank
        @Size(min = 6, max = 32)
        @Schema(description = "Issued by the registrar. Carries the role the account receives.",
                example = "KL-20262-7F3A9C")
        String invitationCode,

        @Pattern(regexp = "^\\d{6,20}$", message = "must be 6 to 20 digits")
        @Schema(example = "506999999")
        String studentCode,

        @Pattern(regexp = "^\\d{1,10}$", message = "must be 1 to 10 digits")
        @Schema(example = "506")
        String programCode
) {
}
