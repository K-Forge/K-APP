package co.edu.konradlorenz.kapp.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

/**
 * Reads the caller's identity from the validated JWT.
 *
 * <p>Services must take identity from here, never from a request header. Trusting a
 * gateway-set header was finding S1 in {@code docs/SECURITY-AUDIT.md}: anyone able to
 * reach a service port directly could forge it. The token is signed, so it cannot be.
 *
 * <p>Token contract, issued by {@code auth-service}:
 * <ul>
 *   <li>{@code sub}   - the stable user id. Not the e-mail, which can change.</li>
 *   <li>{@code email} - the account e-mail, for display and logging.</li>
 *   <li>{@code roles} - already prefixed, e.g. {@code ["ROLE_STUDENT"]}.</li>
 * </ul>
 */
public final class CurrentUser {

    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";

    /**
     * @return the caller's user id
     * @throws IllegalStateException if there is no authenticated JWT, which means the
     *                               endpoint was left unsecured by mistake
     */
    public static String id() {
        return jwt().getSubject();
    }

    public static Optional<String> idIfPresent() {
        return jwtIfPresent().map(Jwt::getSubject);
    }

    public static String email() {
        return jwt().getClaimAsString(CLAIM_EMAIL);
    }

    public static List<String> roles() {
        return jwtIfPresent()
                .map(j -> j.getClaimAsStringList(CLAIM_ROLES))
                .orElseGet(() -> authentication()
                        .map(a -> a.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList())
                        .orElse(List.of()));
    }

    public static boolean hasRole(String role) {
        return roles().contains(role);
    }

    public static Jwt jwt() {
        return jwtIfPresent().orElseThrow(() -> new IllegalStateException(
                "No authenticated JWT in the security context. This endpoint should be secured."));
    }

    public static Optional<Jwt> jwtIfPresent() {
        return authentication()
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast);
    }

    private static Optional<Authentication> authentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(ctx -> ctx.getAuthentication())
                .filter(Authentication::isAuthenticated);
    }

    private CurrentUser() {
    }
}
