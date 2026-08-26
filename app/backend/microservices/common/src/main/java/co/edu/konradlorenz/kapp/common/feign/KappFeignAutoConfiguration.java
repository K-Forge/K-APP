package co.edu.konradlorenz.kapp.common.feign;

import co.edu.konradlorenz.kapp.common.security.CurrentUser;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

/**
 * Propagates the caller's identity across Feign calls.
 *
 * <p>Without this, a service-to-service call arrives with no credentials and every
 * downstream service - each now an independent resource server - answers 401. Forwarding
 * the caller's own token keeps the identity and the roles intact for the whole hop, so
 * authorisation is enforced consistently instead of being dropped at the boundary.
 *
 * <p>Only applies when Feign is on the classpath, which is why the dependency is
 * {@code optional} in this module's pom.
 *
 * <p>Registration is the exception: no user token exists yet while an account is being
 * created, so {@code auth-service} calls {@code POST /internal/users} with
 * {@link InternalTokenInterceptor} instead.
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class KappFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "kappBearerTokenPropagationInterceptor")
    public RequestInterceptor kappBearerTokenPropagationInterceptor() {
        return template -> {
            if (template.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
                return;
            }
            CurrentUser.jwtIfPresent().ifPresent(jwt ->
                    template.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()));
        };
    }
}
