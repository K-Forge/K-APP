package co.edu.konradlorenz.kapp.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Authenticates a service-to-service call that cannot carry a user token.
 *
 * <p>There is exactly one such call in the MVP: {@code auth-service} asking
 * {@code user-service} to create a profile during registration. At that moment the
 * account does not exist yet, so there is no JWT to forward and the normal propagation
 * interceptor has nothing to send.
 *
 * <p>This is a shared secret, not an identity claim, and it only reaches endpoints under
 * {@code /internal/**} which the gateway does not route and which are unreachable from
 * outside the Docker network. That is a narrower and more defensible seam than the
 * {@code X-User-Email} header this architecture replaced, where any caller could assert
 * who they were.
 *
 * <p>Deliberately NOT a global bean: attaching it to every Feign client would leak the
 * secret to every downstream service. Wire it only on the client that needs it:
 *
 * <pre>
 * &#64;FeignClient(name = "user-service", configuration = InternalUserClientConfig.class)
 * </pre>
 */
public class InternalTokenInterceptor implements RequestInterceptor {

    public static final String HEADER = "X-Internal-Token";

    private final String token;

    public InternalTokenInterceptor(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Internal token must not be blank. Set KAPP_INTERNAL_TOKEN in the environment.");
        }
        this.token = token;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(HEADER, token);
    }
}
