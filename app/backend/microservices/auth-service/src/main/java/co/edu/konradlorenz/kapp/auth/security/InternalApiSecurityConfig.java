package co.edu.konradlorenz.kapp.auth.security;

import co.edu.konradlorenz.kapp.common.security.InternalTokenAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * A second security chain, for the one path in this service that is not authenticated by
 * a user's token: {@code /internal/credentials/**}, which user-service calls when an
 * administrator deactivates an account. Sign-in is decided here, so the switch that stops
 * it has to reach here - the profile flag in user-service governs a listing, not access.
 *
 * <p>Character for character the same chain as user-service's, and deliberately so: the
 * shape below is load-bearing in ways that are not obvious, and the comments explaining why
 * are worth more repeated than abbreviated.
 *
 * <h2>The shape of this bean is load-bearing</h2>
 * <ul>
 *   <li><strong>{@code securityMatcher("/internal/**")}</strong> confines it to the path
 *       it is about. A chain without a matcher claims every request, which would put it
 *       ahead of the shared catch-all in {@code common} and leave every other route in
 *       this service unauthenticated - Spring Security now rejects that at startup rather
 *       than letting it happen quietly, but the matcher is what makes the chain correct,
 *       not merely accepted.</li>
 *   <li><strong>An explicit {@code @Order}</strong> puts it ahead of the catch-all, which
 *       sits at {@code LOWEST_PRECEDENCE}. A chain with no {@code @Order} defaults to the
 *       same value and ties with it, so which one wins is undefined.</li>
 * </ul>
 *
 * <p>Everything this chain does not claim falls through to the catch-all and stays
 * protected automatically. Nothing here restates the default, and nothing here may.
 *
 * <p>{@code permitAll} is not "open": authentication happens in
 * {@link InternalTokenAuthenticationFilter}, which runs before authorisation and answers
 * 401 itself. Spring Security has nothing to check because there is no user and no role
 * involved - the secret identifies a service, not a person.
 */
@Configuration
public class InternalApiSecurityConfig {

    /**
     * Ahead of the shared catch-all, and behind the order a test uses for a chain of its
     * own over the same path, so a test can stand in for this one without a tie.
     */
    public static final int INTERNAL_CHAIN_ORDER = 20;

    @Bean
    @Order(INTERNAL_CHAIN_ORDER)
    public SecurityFilterChain internalApiSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            @Value("${kapp.internal.token:}") String internalToken) throws Exception {

        return http
                .securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(
                        new InternalTokenAuthenticationFilter(internalToken, objectMapper),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
