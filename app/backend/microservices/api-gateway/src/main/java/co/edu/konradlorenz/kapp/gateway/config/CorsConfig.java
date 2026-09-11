package co.edu.konradlorenz.kapp.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS with an explicit allow-list.
 *
 * <p>Replaces {@code setAllowedOrigins(List.of("*"))}, which was finding S3: it let any
 * site on the internet call this API from a logged-in user's browser.
 *
 * <p>Origins come from configuration so that adding the admin UI's address is a
 * deployment change rather than a code change. Two notes worth keeping in mind:
 * <ul>
 *   <li>{@code setAllowedOriginPatterns} rather than {@code setAllowedOrigins}, because
 *       the latter combined with {@code allowCredentials(true)} throws at runtime rather
 *       than at startup.</li>
 *   <li>The native Android and iOS clients are not browsers and are not subject to CORS
 *       at all, so tightening this costs the actual product nothing.</li>
 * </ul>
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${kapp.cors.allowed-origins:http://localhost:4200,http://localhost:5173}")
            List<String> allowedOrigins) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
