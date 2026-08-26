package co.edu.konradlorenz.kapp.auth.web;

import co.edu.konradlorenz.kapp.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Public authentication endpoints.
 *
 * <p>Registration, e-mail verification and invitation codes are Phase 1 work and are
 * specified in {@code docs/api/auth.openapi.yaml}, which the mobile clients already
 * build against through the Prism mock.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for an access token")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        var issued = authService.login(request.email(), request.password());
        return new TokenResponse(
                issued.accessToken(), issued.tokenType(), issued.expiresIn(),
                issued.userId(), issued.roles());
    }

    @GetMapping("/health")
    @Operation(summary = "Liveness probe")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "auth-service");
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record TokenResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            String userId,
            List<String> roles) {
    }
}
