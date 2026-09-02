package co.edu.konradlorenz.kapp.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Identity of a freshly created account.
 *
 * <p>{@code emailVerified} is always {@code false} here. Not "usually" - there is no
 * registration path that produces a confirmed address, because confirming one requires
 * following a link that has only just been sent.
 *
 * @param userId the profile id from user-service, which is also the {@code sub} claim of
 *               every token this account will ever receive
 */
public record RegistrationResponse(

        @Schema(example = "3f8a1c2e-7b4d-4e5a-9c6f-2d1b8e0a4c73")
        String userId,

        @Schema(example = "brian.vargasc@konradlorenz.edu.co")
        String email,

        @Schema(description = "Always false immediately after registration.", example = "false")
        boolean emailVerified
) {
}
