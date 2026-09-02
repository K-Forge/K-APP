package co.edu.konradlorenz.kapp.user.web;

import co.edu.konradlorenz.kapp.user.service.UserProfileService;
import co.edu.konradlorenz.kapp.user.web.dto.InternalUserUpsertRequest;
import co.edu.konradlorenz.kapp.user.web.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The one endpoint another service calls directly.
 *
 * <p>auth-service invokes it during registration, at the moment when the account exists
 * as a credential but has no profile and no token has been issued for it - so there is no
 * JWT to forward and the usual propagation has nothing to send. Authentication is the
 * shared {@code X-Internal-Token} secret instead, checked by
 * {@code InternalTokenAuthenticationFilter} in a chain of its own.
 *
 * <p>The gateway does not route {@code /internal/**}, so this path is only reachable from
 * inside the Docker network. No web, Kotlin or Swift client should ever call it.
 *
 * <p>It answers 200 whether it created or updated, and never 409, which is what makes a
 * replayed registration safe.
 */
@RestController
@RequestMapping("/internal/users")
@Tag(name = "Internal")
public class InternalUserController {

    private final UserProfileService users;

    public InternalUserController(UserProfileService users) {
        this.users = users;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create or update a profile from another service",
            description = "Idempotent upsert keyed by e-mail. Returns 200 in both cases.")
    public UserProfileResponse upsert(@Valid @RequestBody InternalUserUpsertRequest request) {
        return users.upsertFromRegistration(request);
    }
}
