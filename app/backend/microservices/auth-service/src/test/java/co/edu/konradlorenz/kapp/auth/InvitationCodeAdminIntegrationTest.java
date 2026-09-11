package co.edu.konradlorenz.kapp.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration of invitation codes: {@code /auth/admin/invitation-codes}.
 *
 * <p>Codes are the only door into KApp that is not a university identity, so the rules
 * worth pinning are the ones that keep that door narrow: no code may grant
 * {@code ROLE_ADMIN}, a code value cannot be reused, and deactivating actually stops a
 * registration rather than only changing what the listing says.
 *
 * <p>Every code these tests create is prefixed {@code KL-TEST-}, which is what
 * {@code AbstractAuthIntegrationTest} clears between tests. The two Mongock-seeded demo
 * codes are shared by the whole suite, so nothing here asserts on the total size of a
 * listing - only on the codes it put there itself.
 */
class InvitationCodeAdminIntegrationTest extends AbstractAuthIntegrationTest {

    private String adminToken() {
        return bearerFor("admin-1", "admin" + INSTITUTIONAL_DOMAIN, "ROLE_ADMIN");
    }

    /** The server mints the code; a caller supplies everything else. */
    private static String requestJson(String role, int maxUses) {
        return """
                {"role":"%s","maxUses":%d,"notes":"intake for the test suite"}
                """.formatted(role, maxUses);
    }

    /** KL- then two groups of four, from an alphabet with no O/0 or I/1. */
    private static final java.util.regex.Pattern MINTED =
            java.util.regex.Pattern.compile("^KL-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$");

    // ── Creating ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("an admin can mint a code, and it comes back unused and active")
    void createReturnsTheMintedCode() throws Exception {
        mockMvc.perform(post("/auth/admin/invitation-codes")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("ROLE_STUDENT", 25)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern(MINTED.pattern())))
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"))
                .andExpect(jsonPath("$.maxUses").value(25))
                .andExpect(jsonPath("$.timesUsed").value(0))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.notes").value("intake for the test suite"));
    }

    @Test
    @DisplayName("a minted code actually works for registration")
    void aMintedCodeCanBeRedeemed() throws Exception {
        // The code has to be read back from the response now, which is the whole point: nobody,
        // including this test, gets to decide what it is.
        String created = mockMvc.perform(post("/auth/admin/invitation-codes")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("ROLE_STUDENT", 5)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String code = com.jayway.jsonpath.JsonPath.read(created, "$.code");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"minted%s","password":"MintedPass2026",
                                 "firstName":"Minted","lastName":"Account",
                                 "invitationCode":"%s",
                                 "studentCode":"506900100","programCode":"506"}
                                """.formatted(INSTITUTIONAL_DOMAIN, code)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("no code may grant ROLE_ADMIN, however it is asked for")
    void adminIsNotGrantableByCode() throws Exception {
        mockMvc.perform(post("/auth/admin/invitation-codes")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("ROLE_ADMIN", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("role"));
    }

    @Test
    @DisplayName("a role outside the grantable set is refused, not silently downgraded")
    void unknownRoleIsRefused() throws Exception {
        mockMvc.perform(post("/auth/admin/invitation-codes")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("ROLE_SUPERUSER", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("role"));
    }

    // A caller cannot ask for a code any more, so a duplicate is not something they can cause.
    // What has to hold instead is that two mints never collide.
    @Test
    @DisplayName("every minted code is different")
    void mintedCodesAreUnique() throws Exception {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 12; i++) {
            String body = mockMvc.perform(post("/auth/admin/invitation-codes")
                            .header("Authorization", adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson("ROLE_STUDENT", 1)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(seen.add(body)).as("a repeated response means a repeated code").isTrue();
        }
    }

    @Test
    @DisplayName("a code a caller tries to choose is ignored, not honoured")
    void aSuppliedCodeIsIgnored() throws Exception {
        // The old contract took the code from the body. Anything still sending one - an old
        // client, a copied curl - must not end up choosing it.
        mockMvc.perform(post("/auth/admin/invitation-codes")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"KL-CHOSEN-BY-HAND","role":"ROLE_STUDENT","maxUses":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not("KL-CHOSEN-BY-HAND")))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern(MINTED.pattern())));
    }

    // ── Listing ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("the listing carries the code's remaining quota, not just its existence")
    void listingReportsUsage() throws Exception {
        seedInvitationCode("KL-TEST-PARTIAL", "ROLE_STUDENT", 10, 4, true, null);

        mockMvc.perform(get("/auth/admin/invitation-codes").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='KL-TEST-PARTIAL')].maxUses").value(10))
                .andExpect(jsonPath("$[?(@.code=='KL-TEST-PARTIAL')].timesUsed").value(4));
    }

    @Test
    @DisplayName("active=true means redeemable now, so a spent code is excluded despite its flag")
    void activeFilterExcludesSpentCodes() throws Exception {
        seedInvitationCode("KL-TEST-SPENT", "ROLE_STUDENT", 2, 2, true, null);
        seedInvitationCode("KL-TEST-FRESH", "ROLE_STUDENT", 2, 0, true, null);

        mockMvc.perform(get("/auth/admin/invitation-codes?active=true")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='KL-TEST-FRESH')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.code=='KL-TEST-SPENT')]").isEmpty());
    }

    @Test
    @DisplayName("active=true excludes an expired code")
    void activeFilterExcludesExpiredCodes() throws Exception {
        seedInvitationCode("KL-TEST-EXPIRED", "ROLE_STUDENT", 5, 0, true,
                Instant.now().minus(1, ChronoUnit.DAYS));

        mockMvc.perform(get("/auth/admin/invitation-codes?active=true")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='KL-TEST-EXPIRED')]").isEmpty());
    }

    @Test
    @DisplayName("active=false returns the codes that cannot be redeemed")
    void inactiveFilterReturnsUnusableCodes() throws Exception {
        seedInvitationCode("KL-TEST-OFF", "ROLE_STUDENT", 5, 0, false, null);

        mockMvc.perform(get("/auth/admin/invitation-codes?active=false")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='KL-TEST-OFF')]").isNotEmpty());
    }

    // ── Deactivating ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivating a code stops registrations, not just the listing")
    void deactivatingBlocksRegistration() throws Exception {
        seedInvitationCode("KL-TEST-REVOKE", "ROLE_STUDENT", 5, 0, true, null);

        mockMvc.perform(patch("/auth/admin/invitation-codes/{code}", "KL-TEST-REVOKE")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"revoked%s","password":"RevokedPass2026",
                                 "firstName":"Revoked","lastName":"Account",
                                 "invitationCode":"KL-TEST-REVOKE",
                                 "studentCode":"506900101","programCode":"506"}
                                """.formatted(INSTITUTIONAL_DOMAIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("deactivating preserves the redemption count, which is why it exists")
    void deactivatingKeepsHistory() throws Exception {
        seedInvitationCode("KL-TEST-USED", "ROLE_STUDENT", 10, 7, true, null);

        mockMvc.perform(patch("/auth/admin/invitation-codes/{code}", "KL-TEST-USED")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timesUsed").value(7));
    }

    @Test
    @DisplayName("deactivating is reversible")
    void reactivatingWorks() throws Exception {
        seedInvitationCode("KL-TEST-TOGGLE", "ROLE_STUDENT", 5, 0, false, null);

        mockMvc.perform(patch("/auth/admin/invitation-codes/{code}", "KL-TEST-TOGGLE")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("patching an unknown code is 404, not a silent create")
    void patchOfUnknownCodeIs404() throws Exception {
        mockMvc.perform(patch("/auth/admin/invitation-codes/{code}", "KL-TEST-GHOST")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a patch body without `active` is rejected")
    void patchWithoutActiveIsRejected() throws Exception {
        seedInvitationCode("KL-TEST-EMPTY", "ROLE_STUDENT", 5, 0, true, null);

        mockMvc.perform(patch("/auth/admin/invitation-codes/{code}", "KL-TEST-EMPTY")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── Deleting ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleting removes the code from the listing")
    void deleteRemovesTheCode() throws Exception {
        seedInvitationCode("KL-TEST-DELETE", "ROLE_STUDENT", 5, 0, true, null);

        mockMvc.perform(delete("/auth/admin/invitation-codes/{code}", "KL-TEST-DELETE")
                        .header("Authorization", adminToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/auth/admin/invitation-codes").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='KL-TEST-DELETE')]").isEmpty());
    }

    @Test
    @DisplayName("deleting an unknown code is 404")
    void deleteOfUnknownCodeIs404() throws Exception {
        mockMvc.perform(delete("/auth/admin/invitation-codes/{code}", "KL-TEST-NOTHERE")
                        .header("Authorization", adminToken()))
                .andExpect(status().isNotFound());
    }

    // ── Authorization ──────────────────────────────────────────────────────────────
    // One method per (endpoint, caller): a regression names the exact broken combination
    // instead of one test failing for an ambiguous reason.

    @Test
    @DisplayName("listing codes without a token is 401")
    void listWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/admin/invitation-codes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a student cannot list codes")
    void studentCannotList() throws Exception {
        mockMvc.perform(get("/auth/admin/invitation-codes")
                        .header("Authorization", bearerFor("s1", "s1" + INSTITUTIONAL_DOMAIN, "ROLE_STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a professor cannot list codes")
    void professorCannotList() throws Exception {
        mockMvc.perform(get("/auth/admin/invitation-codes")
                        .header("Authorization", bearerFor("p1", "p1" + INSTITUTIONAL_DOMAIN, "ROLE_PROFESSOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a guest cannot list codes")
    void guestCannotList() throws Exception {
        mockMvc.perform(get("/auth/admin/invitation-codes")
                        .header("Authorization", bearerFor("g1", "g1@gmail.com", "ROLE_GUEST")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("creating a code without a token is 401")
    void createWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/admin/invitation-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("ROLE_STUDENT", 1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a student cannot mint a code")
    void studentCannotCreate() throws Exception {
        mockMvc.perform(post("/auth/admin/invitation-codes")
                        .header("Authorization", bearerFor("s2", "s2" + INSTITUTIONAL_DOMAIN, "ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("ROLE_STUDENT", 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a professor cannot mint a code")
    void professorCannotCreate() throws Exception {
        mockMvc.perform(post("/auth/admin/invitation-codes")
                        .header("Authorization", bearerFor("p2", "p2" + INSTITUTIONAL_DOMAIN, "ROLE_PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("ROLE_STUDENT", 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deactivating without a token is 401")
    void patchWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(patch("/auth/admin/invitation-codes/{code}", "KL-20262-STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a student cannot deactivate a code")
    void studentCannotPatch() throws Exception {
        seedInvitationCode("KL-TEST-PROTECTED", "ROLE_STUDENT", 5, 0, true, null);

        mockMvc.perform(patch("/auth/admin/invitation-codes/{code}", "KL-TEST-PROTECTED")
                        .header("Authorization", bearerFor("s3", "s3" + INSTITUTIONAL_DOMAIN, "ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleting without a token is 401")
    void deleteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/auth/admin/invitation-codes/{code}", "KL-20262-STUDENT"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a student cannot delete a code")
    void studentCannotDelete() throws Exception {
        seedInvitationCode("KL-TEST-SAFE", "ROLE_STUDENT", 5, 0, true, null);

        mockMvc.perform(delete("/auth/admin/invitation-codes/{code}", "KL-TEST-SAFE")
                        .header("Authorization", bearerFor("s4", "s4" + INSTITUTIONAL_DOMAIN, "ROLE_STUDENT")))
                .andExpect(status().isForbidden());
    }
}
