package co.edu.konradlorenz.kapp.auth.web;

import co.edu.konradlorenz.kapp.auth.service.CredentialStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The one endpoint user-service calls directly.
 *
 * <p>An administrator deactivating an account in the portal reaches
 * {@code PATCH /api/users/{userId}/status} in user-service, which owns the profile. But whether
 * the account may sign in is decided here, so user-service forwards the same decision to this
 * endpoint in the same operation. Either both halves happen or the call fails - a profile marked
 * inactive whose owner can still sign in is exactly the bug this replaced.
 *
 * <p>The gateway does not route {@code /internal/**}, so this is reachable only from inside the
 * Docker network, and is authenticated with the shared {@code X-Internal-Token} rather than a
 * user's bearer token.
 *
 * <p><strong>POST, not PATCH</strong>, though a partial update is what this is. Feign's default
 * client is {@code java.net.HttpURLConnection}, which throws
 * {@code ProtocolException: Invalid HTTP method: PATCH} - the JDK has refused that verb since
 * before it was standard and still does. The alternatives are a different HTTP client on the
 * calling service or the right verb here; a dependency in the critical path is a poor trade for
 * a verb on an endpoint that is not in any public contract and has exactly one caller.
 */
@RestController
@RequestMapping("/internal/credentials")
@Tag(name = "Internal")
public class InternalCredentialController {

    private final CredentialStatusService credentials;

    public InternalCredentialController(CredentialStatusService credentials) {
        this.credentials = credentials;
    }

    @PostMapping(path = "/{userId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Allow or refuse sign-in for an account",
            description = "Idempotent. Does not revoke a token already issued; it stops the "
                    + "next one being issued.")
    public void setStatus(@PathVariable String userId,
                          @Valid @RequestBody InternalCredentialStatusRequest request) {
        credentials.setSignInAllowed(userId, request.active());
    }
}
