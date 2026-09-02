package co.edu.konradlorenz.kapp.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Address a new verification e-mail should be sent to.
 */
public record ResendVerificationRequest(

        @NotBlank
        @Email
        @Size(max = 100)
        @Schema(example = "brian.vargasc@konradlorenz.edu.co")
        String email
) {
}
