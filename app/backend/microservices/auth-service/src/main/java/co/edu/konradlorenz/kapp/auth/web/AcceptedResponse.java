package co.edu.konradlorenz.kapp.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Neutral acknowledgement.
 *
 * <p>The wording is identical for an existing address, an unknown one and one that was
 * already confirmed - which is the entire point, and why this is a fixed constant rather
 * than a message the caller composes per case.
 */
public record AcceptedResponse(

        @Schema(example = "If an account exists for that address, a verification e-mail has been sent.")
        String message
) {
}
