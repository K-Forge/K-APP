package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Proves the service is reachable AND that the caller carried a valid token.
 *
 * <p>This is the endpoint the Phase 0 exit criteria are checked against: a valid JWT
 * returns 200, no token returns 401, and hitting the service port directly from the host
 * is refused because the port is not published. It stays in place after the real
 * endpoints land, as a cheap smoke test.
 */
@RestController
@RequestMapping("/api/schedule")
@Tag(name = "Diagnostics")
public class PingController {

    @GetMapping("/ping")
    @Operation(summary = "Authenticated liveness probe",
            description = "Returns the caller's identity as resolved from the JWT.")
    public Map<String, Object> ping() {
        return Map.of(
                "service", "schedule-service",
                "userId", CurrentUser.id(),
                "roles", CurrentUser.roles());
    }
}
