package co.edu.konradlorenz.kapp.auth.web;

import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PATCH /internal/credentials/{userId}/status}.
 *
 * @param active true to allow sign-in, false to suspend the account. Boxed and {@code @NotNull}
 *               so an absent field is a 400 rather than a silent {@code false} - the difference
 *               between "reactivate" and "omitted" must not be a locked-out account.
 */
public record InternalCredentialStatusRequest(@NotNull Boolean active) {
}
