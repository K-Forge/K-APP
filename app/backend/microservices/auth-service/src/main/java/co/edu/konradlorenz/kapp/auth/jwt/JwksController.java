package co.edu.konradlorenz.kapp.auth.jwt;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publishes the public signing key so the other services can verify tokens.
 *
 * <p>This endpoint is the hinge of the whole security design. Each of user-, semaphore-,
 * schedule- and map-service is an independent OAuth2 resource server pointed at this URL
 * through {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}. Because they
 * verify a signature rather than trust a header the gateway set, reaching a service port
 * directly buys an attacker nothing - which is what closes finding S1.
 *
 * <p>Must stay public and unauthenticated: a service cannot authenticate in order to
 * learn how to authenticate.
 */
@RestController
@Tag(name = "Keys")
public class JwksController {

    private final RsaKeyProvider keys;

    public JwksController(RsaKeyProvider keys) {
        this.keys = keys;
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "JSON Web Key Set",
            description = "Public RSA key used to verify KApp access tokens.")
    public Map<String, Object> jwks() {
        return keys.publicJwkSet().toJSONObject();
    }
}
