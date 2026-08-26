package co.edu.konradlorenz.kapp.auth.config;

import co.edu.konradlorenz.kapp.common.security.KappSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * auth-service is the one service with genuinely public endpoints, so it replaces the
 * default chain from {@code common} rather than inheriting it. Declaring this bean makes
 * the shared {@code @ConditionalOnMissingBean} chain back off.
 *
 * <p>It is still a resource server as well: once signed in, a caller uses the same token
 * here as everywhere else.
 */
@Configuration
public class AuthSecurityConfig {

    /**
     * Endpoints reachable without a token. Kept deliberately short - every entry is a
     * door. The JWKS endpoint has to be here: a service cannot authenticate in order to
     * discover how to authenticate.
     */
    private static final String[] PUBLIC_AUTH_PATHS = {
            "/auth/login",
            "/auth/register",
            "/auth/register/guest",
            "/auth/verify",
            "/auth/verify/resend",
            "/auth/health",
            "/.well-known/jwks.json"
    };

    @Bean
    public SecurityFilterChain authSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AuthenticationEntryPoint entryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(KappSecurityAutoConfiguration.PUBLIC_PATHS).permitAll()
                        .requestMatchers(PUBLIC_AUTH_PATHS).permitAll()
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

    /**
     * BCrypt at strength 12. The default of 10 dates from much slower hardware; 12 keeps
     * a single verification in the tens of milliseconds while making offline cracking
     * substantially more expensive.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
