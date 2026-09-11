package co.edu.konradlorenz.kapp.auth.web;

import co.edu.konradlorenz.kapp.auth.domain.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What the visitor presents at the counter.
 *
 * <p>The document is required, not optional. The pass exists so reception has a record of
 * who was in the building; a pass redeemed anonymously would be an account with extra steps.
 */
@Schema(name = "VisitorPassRedemptionRequest")
public record VisitorPassRedemptionRequest(
        @NotNull DocumentType documentType,

        @Schema(description = "Digits and letters only. Stored as given, deleted after 30 days.",
                example = "1032456789")
        @NotBlank @Size(min = 4, max = 20)
        @Pattern(regexp = "^[A-Za-z0-9]+$", message = "must contain only letters and digits")
        String documentNumber,

        @Schema(example = "Pepita Perez")
        @NotBlank @Size(max = 120) String visitorName
) {
}
