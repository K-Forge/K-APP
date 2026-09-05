package co.edu.konradlorenz.kapp.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * What reception sends to mint a pass. Everything is optional: the point of the counter is
 * that issuing a pass takes one click.
 */
@Schema(name = "VisitorPassRequest")
public record VisitorPassRequest(
        @Schema(description = "Free text for whoever issued it — which event, which office.",
                example = "Feria de programas, auditorio")
        @Size(max = 200) String notes
) {
}
