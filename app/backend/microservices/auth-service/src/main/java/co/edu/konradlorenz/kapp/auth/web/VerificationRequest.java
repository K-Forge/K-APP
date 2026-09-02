package co.edu.konradlorenz.kapp.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The single-use token from the verification e-mail. Not a JWT access token.
 */
public record VerificationRequest(

        @NotBlank
        @Size(min = 16, max = 256)
        @Schema(example = "8f14e45fceea167a5a36dedd4bea2543c1e9b1f0a7d24c5e")
        String token
) {
}
