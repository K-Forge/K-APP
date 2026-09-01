package co.edu.konradlorenz.kapp.common.security;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.List;

/**
 * Wires resource-server security identically in every KApp service.
 *
 * <p>Delivered as auto-configuration, not as scanned components: each service's
 * {@code @SpringBootApplication} sits in its own package and would not pick these up.
 * This way no service has to remember anything, and all of them get byte-identical
 * role mapping and error envelopes.
 *
 * <h2>Why every service validates the token itself</h2>
 * Finding S1 in {@code docs/SECURITY-AUDIT.md}: the gateway used to be the only place a
 * token was checked, and services trusted an {@code X-User-Email} header it set. Anyone
 * who could reach a service port directly could forge that header. Now each service is
 * an OAuth2 resource server, so bypassing the gateway gains nothing.
 *
 * <h2>Configuration each service must provide</h2>
 * <pre>
 * spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://auth-service:8081/.well-known/jwks.json
 * </pre>
 * Use {@code jwk-set-uri}, never {@code issuer-uri}. {@code issuer-uri} performs OIDC
 * discovery while the bean is being created, so every service would fail to start unless
 * auth-service were already up - a boot-order deadlock under {@code docker compose up}
 * that surfaces as an opaque {@code IllegalArgumentException}. {@code jwk-set-uri}
 * fetches lazily, on the first token it has to verify.
 *
 * <p>RS256 with a published JWKS, rather than a shared symmetric secret, for two reasons:
 * a symmetric key handed to seven services is seven places that can MINT admin tokens,
 * not just verify them; and Microsoft Entra ID signs RS256 and publishes a JWKS, so the
 * planned migration becomes a change of property value instead of a rewrite.
 */
@AutoConfiguration
@ConditionalOnClass({SecurityFilterChain.class, JwtAuthenticationConverter.class})
@EnableMethodSecurity
public class KappSecurityAutoConfiguration {

    /**
     * Paths every service leaves open: health probes for Docker, and the generated
     * OpenAPI document plus its UI, which the gateway aggregates.
     */
    public static final String[] PUBLIC_PATHS = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };

    /**
     * Maps the {@code roles} claim onto Spring authorities.
     *
     * <p>The claim already carries the {@code ROLE_} prefix, so the authority prefix is
     * set to the empty string. Leaving the default {@code "SCOPE_"} would make every
     * {@code hasRole(...)} check fail silently, which is exactly the kind of bug that
     * looks like a data problem for a day.
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(CurrentUser.CLAIM_ROLES);
        authorities.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /**
     * Renders a missing or invalid token as {@link ApiError}, so an unauthenticated
     * response looks like every other error the API returns.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticationEntryPoint kappAuthenticationEntryPoint(ObjectMapper mapper) {
        return (request, response, authException) -> writeError(mapper, response,
                HttpStatus.UNAUTHORIZED, "Authentication required", request.getRequestURI());
    }

    /**
     * Renders an authorisation failure from the filter chain as {@link ApiError}.
     * Denials from {@code @PreAuthorize} are handled by {@code GlobalExceptionHandler}.
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessDeniedHandler kappAccessDeniedHandler(ObjectMapper mapper) {
        return (request, response, deniedException) -> writeError(mapper, response,
                HttpStatus.FORBIDDEN, "You are not allowed to perform this action",
                request.getRequestURI());
    }

    /**
     * The catch-all chain: everything requires a valid token except health and the API
     * docs.
     *
     * <p>Ordered last on purpose, and deliberately NOT {@code @ConditionalOnMissingBean}.
     * It used to be conditional, which meant a service declaring any chain of its own -
     * even one that only covered {@code /internal/**} - silently switched this one off
     * and left every other route unauthenticated, with nothing failing loudly. That is a
     * security hole produced by an ordinary-looking edit, so the mechanism is gone.
     *
     * <p>A service that needs different rules for some paths adds its own chain with a
     * {@code securityMatcher} limiting it to those paths, and an explicit
     * {@code @Order} ahead of this one:
     *
     * <pre>
     * &#64;Bean
     * &#64;Order(10)
     * SecurityFilterChain internalChain(HttpSecurity http) throws Exception {
     *     return http.securityMatcher("/internal/**")
     *                ...
     *                .build();
     * }
     * </pre>
     *
     * Spring Security tries each chain in order and uses the first whose matcher accepts
     * the request, so anything the narrow chain does not claim still lands here and stays
     * protected. An explicit {@code @Order} is required: a chain without one also defaults
     * to {@code LOWEST_PRECEDENCE} and ties with this bean.
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain kappCatchAllSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AuthenticationEntryPoint entryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    private static void writeError(ObjectMapper mapper,
                                   jakarta.servlet.http.HttpServletResponse response,
                                   HttpStatus status, String message, String path)
            throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, path,
                List.of());
        mapper.writeValue(response.getOutputStream(), body);
    }
}
