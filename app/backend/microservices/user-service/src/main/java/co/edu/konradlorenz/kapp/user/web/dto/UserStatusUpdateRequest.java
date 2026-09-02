package co.edu.konradlorenz.kapp.user.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PATCH /api/users/{userId}/status}.
 *
 * <p>A boxed {@link Boolean} with {@code @NotNull}, never a primitive: a primitive would
 * silently default an absent {@code active} to {@code false} and deactivate the account
 * the administrator was only inspecting.
 */
public record UserStatusUpdateRequest(

        @NotNull(message = "must not be null")
        Boolean active
) {
}
