package co.edu.konradlorenz.kapp.auth;

import co.edu.konradlorenz.kapp.auth.domain.Credential;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code PATCH /internal/credentials/{userId}/status} — the half of "deactivate" that actually
 * takes access away.
 *
 * <p>Before this endpoint existed, deactivating an account in the admin portal wrote
 * {@code active = false} on the profile in user-service and nothing else. Sign-in is decided
 * here, only ever refuses {@code SUSPENDED}, and nothing in the product ever set
 * {@code SUSPENDED} — so the button said it removed access and the person kept logging in.
 */
class CredentialStatusIntegrationTest extends AbstractAuthIntegrationTest {

    private static final String EMAIL = "suspendable@konradlorenz.edu.co";
    private static final String PASSWORD = "ExamplePassword123";

    private String login() throws Exception {
        return """
                {"email": "%s", "password": "%s"}""".formatted(EMAIL, PASSWORD);
    }

    @Test
    @DisplayName("suspending a credential stops the next sign-in")
    void suspendingStopsSignIn() throws Exception {
        Credential seeded = seedCredential(EMAIL, PASSWORD, List.of("ROLE_STUDENT"),
                Credential.Status.ACTIVE, true);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(login()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/credentials/{userId}/status", seeded.userId())
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": false}"""))
                .andExpect(status().isNoContent());

        assertThat(credentials.findByUserId(seeded.userId()).orElseThrow().status())
                .isEqualTo(Credential.Status.SUSPENDED);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(login()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("restoring a credential lets it sign in again")
    void restoringLetsItSignInAgain() throws Exception {
        Credential seeded = seedCredential(EMAIL, PASSWORD, List.of("ROLE_STUDENT"),
                Credential.Status.SUSPENDED, true);

        mockMvc.perform(post("/internal/credentials/{userId}/status", seeded.userId())
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": true}"""))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(login()))
                .andExpect(status().isOk());
    }

    // Verification is off in the MVP, so every account carries emailVerified = false. Sending
    // those back to PENDING_VERIFICATION on restore would make deactivation a one-way door.
    @Test
    @DisplayName("an unverified account can still be restored, because verification is off")
    void restoringAnUnverifiedAccountStillWorks() throws Exception {
        Credential seeded = seedCredential(EMAIL, PASSWORD, List.of("ROLE_STUDENT"),
                Credential.Status.SUSPENDED, false);

        mockMvc.perform(post("/internal/credentials/{userId}/status", seeded.userId())
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": true}"""))
                .andExpect(status().isNoContent());

        assertThat(credentials.findByUserId(seeded.userId()).orElseThrow().status())
                .isEqualTo(Credential.Status.ACTIVE);
    }

    @Test
    @DisplayName("setting the state it already holds writes nothing and still answers 204")
    void isIdempotent() throws Exception {
        Credential seeded = seedCredential(EMAIL, PASSWORD, List.of("ROLE_STUDENT"),
                Credential.Status.ACTIVE, true);

        mockMvc.perform(post("/internal/credentials/{userId}/status", seeded.userId())
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": true}"""))
                .andExpect(status().isNoContent());

        // Truncated because MongoDB stores instants to the millisecond while the seeded object
        // still carries the microseconds Instant.now() produced. Same write, different precision.
        assertThat(credentials.findByUserId(seeded.userId()).orElseThrow().updatedAt())
                .isEqualTo(seeded.updatedAt().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    @DisplayName("without the internal token the endpoint is 401, not a silent success")
    void withoutTheInternalTokenItIs401() throws Exception {
        Credential seeded = seedCredential(EMAIL, PASSWORD, List.of("ROLE_STUDENT"),
                Credential.Status.ACTIVE, true);

        mockMvc.perform(post("/internal/credentials/{userId}/status", seeded.userId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": false}"""))
                .andExpect(status().isUnauthorized());

        assertThat(credentials.findByUserId(seeded.userId()).orElseThrow().status())
                .isEqualTo(Credential.Status.ACTIVE);
    }

    @Test
    @DisplayName("an unknown user id is a 404, because the two services disagreeing matters")
    void unknownUserIdIs404() throws Exception {
        mockMvc.perform(post("/internal/credentials/{userId}/status", "no-such-user")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": false}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("an absent active flag is a 400, never a defaulted false")
    void absentFlagIs400() throws Exception {
        Credential seeded = seedCredential(EMAIL, PASSWORD, List.of("ROLE_STUDENT"),
                Credential.Status.ACTIVE, true);

        mockMvc.perform(post("/internal/credentials/{userId}/status", seeded.userId())
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(credentials.findByUserId(seeded.userId()).orElseThrow().status())
                .isEqualTo(Credential.Status.ACTIVE);
    }
}
