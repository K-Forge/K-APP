package co.edu.konradlorenz.kapp.user.web;

import co.edu.konradlorenz.kapp.common.security.CurrentUser;
import co.edu.konradlorenz.kapp.user.service.UserProfileService;
import co.edu.konradlorenz.kapp.user.web.dto.UserProfileResponse;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service access to the caller's own profile.
 *
 * <p>Neither method takes a user id. The target is always the {@code sub} claim of the
 * validated token, so there is no parameter an attacker could point at somebody else's
 * account, and no ownership check that could be forgotten. Any authenticated role reaches
 * its own profile here, guests included.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Profile")
public class UserProfileController {

    private final UserProfileService users;
    private final ProfilePatchReader patchReader;

    public UserProfileController(UserProfileService users, ProfilePatchReader patchReader) {
        this.users = users;
        this.patchReader = patchReader;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile",
            description = "Resolved from the token's sub claim. academic is null for a guest.")
    public UserProfileResponse me() {
        return users.byId(CurrentUser.id());
    }

    /**
     * Binds the raw JSON tree on purpose.
     *
     * <p>A record would make an omitted field and a field sent as {@code null} arrive as
     * the same thing, and the contract gives them opposite meanings: leave it alone
     * versus clear it. {@link ProfilePatchReader} reads the tree and hands back a patch
     * that keeps the two apart.
     */
    @PatchMapping(path = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update the authenticated user's profile",
            description = "Only the fields present in the body are touched. Sending null "
                    + "clears phone, avatarUrl, identification or academic. Sending email, "
                    + "role, active or id is rejected with 400.")
    public UserProfileResponse updateMe(@RequestBody JsonNode body) {
        return users.update(CurrentUser.id(), patchReader.read(body));
    }
}
