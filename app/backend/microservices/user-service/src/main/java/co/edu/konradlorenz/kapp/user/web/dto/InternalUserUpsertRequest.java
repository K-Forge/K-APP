package co.edu.konradlorenz.kapp.user.web.dto;

import co.edu.konradlorenz.kapp.user.domain.AcademicInfo;
import co.edu.konradlorenz.kapp.user.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /internal/users}, sent by auth-service during registration.
 *
 * <p>{@code email} is the natural key of the upsert, which is why a repeated call updates
 * rather than duplicates and the registration flow can be replayed after a timeout.
 *
 * <p>{@code academic} carries no {@code @NotNull}: it is required to be <em>present</em>
 * in the JSON but may be null, and the rule that decides which is correct depends on the
 * role, so it is enforced in the service rather than by an annotation.
 */
public record InternalUserUpsertRequest(

        @NotBlank(message = "must not be blank")
        @Email(message = "must be a well-formed e-mail address")
        @Size(max = 100, message = "must be at most 100 characters")
        String email,

        @NotBlank(message = "must not be blank")
        @Size(min = 1, max = 50, message = "must be between 1 and 50 characters")
        String firstName,

        @NotBlank(message = "must not be blank")
        @Size(min = 1, max = 50, message = "must be between 1 and 50 characters")
        String lastName,

        @NotNull(message = "must not be null")
        UserRole role,

        @Valid
        AcademicInfo academic
) {
}
