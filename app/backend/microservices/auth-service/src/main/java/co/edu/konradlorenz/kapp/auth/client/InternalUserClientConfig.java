package co.edu.konradlorenz.kapp.auth.client;

import co.edu.konradlorenz.kapp.common.feign.InternalTokenInterceptor;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Per-client configuration for {@link UserProfileClient}.
 *
 * <p><strong>Deliberately not annotated {@code @Configuration}.</strong> Feign gives each
 * client its own child context and applies this class only there. Were it a
 * {@code @Configuration} it would be component-scanned into the main context instead, and
 * the interceptor would attach the internal secret to <em>every</em> Feign call this
 * service ever makes. As it stands the secret reaches exactly one endpoint on one service.
 *
 * <p>Timeouts are short and retries are off on purpose. Registration is a user-facing
 * request: waiting the default 60 seconds on an unreachable user-service is worse than
 * failing in three, and a retry on a POST that is already idempotent buys nothing that the
 * user pressing the button again does not.
 */
public class InternalUserClientConfig {

    private static final Logger log = LoggerFactory.getLogger(InternalUserClientConfig.class);

    /** The value {@code docker-compose.yml} falls back to when nothing is set in {@code .env}. */
    private static final String UNSET_PLACEHOLDER = "CHANGE_ME_UNSET_INTERNAL_TOKEN";

    @Bean
    public RequestInterceptor internalTokenInterceptor(
            @Value("${kapp.internal.token:}") String token) {
        if (UNSET_PLACEHOLDER.equals(token)) {
            log.warn("""
                    KAPP_INTERNAL_TOKEN is still the compose placeholder. user-service will \
                    reject the profile upsert and registration will answer 503. Set a real \
                    value in .env, the same one on both services.""");
        }
        return new InternalTokenInterceptor(token);
    }

    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public Request.Options options() {
        return new Request.Options(3, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }
}
