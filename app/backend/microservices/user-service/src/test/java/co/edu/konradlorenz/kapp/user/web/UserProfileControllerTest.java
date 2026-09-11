package co.edu.konradlorenz.kapp.user.web;

import co.edu.konradlorenz.kapp.user.AbstractUserServiceTest;
import co.edu.konradlorenz.kapp.user.domain.UserProfile;
import co.edu.konradlorenz.kapp.user.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Self-service access to {@code /api/users/me}: what the caller sees, and what a partial
 * update is and is not allowed to touch.
 *
 * <p>Who may call these endpoints at all is covered by {@link UserAuthorizationMatrixTest};
 * this class is about the shape of the response and the semantics of the patch.
 */
class UserProfileControllerTest extends AbstractUserServiceTest {

    private static final String STUDENT_ID = "b0000000-0000-0000-0000-000000000001";
    private static final String GUEST_ID = "b0000000-0000-0000-0000-000000000002";
    /** Only used to read a guest's profile, which is no longer readable through /me. */
    private static final String ADMIN_ID = "b0000000-0000-0000-0000-000000000003";

    // ---------------------------------------------------------------------------------
    // GET /api/users/me
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("a student's own profile carries the full academic record")
    void getMe_student_returnsAcademicRecord() throws Exception {
        save(fullyPopulated(STUDENT_ID, "pepito.perez@konradlorenz.edu.co"));

        mockMvc.perform(get("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(STUDENT_ID))
                .andExpect(jsonPath("$.email").value("pepito.perez@konradlorenz.edu.co"))
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"))
                .andExpect(jsonPath("$.academic.studentCode").value("506999999"))
                .andExpect(jsonPath("$.academic.currentLevel").value(6));
    }

    /**
     * A guest profile can still exist — an administrator can create one — and it must still
     * serialise {@code academic} as an explicit null rather than omitting the key, because a
     * client distinguishes "no academic record" from "field not sent". It is read through the
     * admin endpoint now: {@code /api/users/me} refuses ROLE_GUEST, since that role means a
     * visitor with no account at all.
     */
    @Test
    @DisplayName("a guest's profile round-trips with academic explicitly null, not absent")
    void guestProfile_academicIsExplicitlyNull() throws Exception {
        save(guest(GUEST_ID, "maria.rodriguez@gmail.com", "Maria", "Rodriguez"));

        mockMvc.perform(get("/api/users/{id}", GUEST_ID)
                        .with(callerWith(ADMIN_ID, UserRole.ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_GUEST"))
                .andExpect(jsonPath("$..academic").exists())
                .andExpect(jsonPath("$.academic").value(nullValue()));
    }

    @Test
    @DisplayName("a visitor's token is refused outright: there is no account behind it")
    void getMe_guest_forbidden() throws Exception {
        mockMvc.perform(get("/api/users/me").with(callerWith(GUEST_ID, UserRole.ROLE_GUEST)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------------------
    // PATCH /api/users/me - partial update semantics
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("a field left out of the body is not touched")
    void patchMe_omittedField_isUnchanged() throws Exception {
        save(fullyPopulated(STUDENT_ID, "pepito.perez@konradlorenz.edu.co"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Updated"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                // lastName was not in the body, so the stored value survives untouched.
                .andExpect(jsonPath("$.lastName").value("Perez Gomez"))
                .andExpect(jsonPath("$.phone").value("+573105551234"));
    }

    @Test
    @DisplayName("an explicit null clears the phone")
    void patchMe_explicitNullPhone_clearsIt() throws Exception {
        save(fullyPopulated(STUDENT_ID, "pepito.perez@konradlorenz.edu.co"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": null}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(nullValue()))
                // Untouched fields, including the ones a null-collapsing bug would wipe.
                .andExpect(jsonPath("$.firstName").value("Pepito"));
    }

    @Test
    @DisplayName("an explicit null clears the identification")
    void patchMe_explicitNullIdentification_clearsIt() throws Exception {
        save(fullyPopulated(STUDENT_ID, "pepito.perez@konradlorenz.edu.co"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identification": null}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identification").value(nullValue()));
    }

    @Test
    @DisplayName("a student may set a new academic record")
    void patchMe_student_canSetAcademic() throws Exception {
        save(student(STUDENT_ID, "pepito.perez@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"academic": {"studentCode": "506999999", "programCode": "506",
                                              "pensumCode": "1015", "currentLevel": 3}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academic.studentCode").value("506999999"))
                .andExpect(jsonPath("$.academic.currentLevel").value(3));
    }

    // ---------------------------------------------------------------------------------
    // PATCH /api/users/me - fields the client may never set here
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("sending email is rejected with 400")
    void patchMe_rejectsEmail() throws Exception {
        save(student(STUDENT_ID, "pepito.perez@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "someone-else@konradlorenz.edu.co"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("email"));
    }

    @Test
    @DisplayName("sending role is rejected with 400")
    void patchMe_rejectsRole() throws Exception {
        save(student(STUDENT_ID, "pepito.perez@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "ROLE_ADMIN"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("role"));
    }

    @Test
    @DisplayName("sending active is rejected with 400")
    void patchMe_rejectsActive() throws Exception {
        save(student(STUDENT_ID, "pepito.perez@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": false}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("active"));
    }

    @Test
    @DisplayName("sending id is rejected with 400")
    void patchMe_rejectsId() throws Exception {
        save(student(STUDENT_ID, "pepito.perez@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": "some-other-id"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("id"));
    }

    @Test
    @DisplayName("an unrecognised field is rejected with 400")
    void patchMe_rejectsUnknownField() throws Exception {
        save(student(STUDENT_ID, "pepito.perez@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "Bri"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("nickname"));
    }

    @Test
    @DisplayName("an empty body is rejected with 400")
    void patchMe_rejectsEmptyBody() throws Exception {
        save(student(STUDENT_ID, "pepito.perez@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------------------------
    // PATCH /api/users/me - the guest / academic business rule
    // ---------------------------------------------------------------------------------

    // "A guest may not be given an academic record" used to be asserted here, through
    // PATCH /api/users/me. That door is closed: ROLE_GUEST now means a visitor with no
    // account, and the endpoint refuses it. The rule itself still holds and is asserted
    // where it is still reachable - InternalUserControllerTest.upsert_guestWithAcademic_isRejected,
    // covering the one path that can still create a ROLE_GUEST profile.

    // ---------------------------------------------------------------------------------
    // PATCH /api/users/me - field-level validation, matching the contract exactly
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("a phone not in E.164 form is rejected with 400")
    void patchMe_rejectsMalformedPhone() throws Exception {
        save(student(STUDENT_ID, "pepito.perez@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "3105551234"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("phone"));
    }

    @Test
    @DisplayName("currentLevel above 12 is rejected with 400")
    void patchMe_rejectsCurrentLevelAboveCap() throws Exception {
        save(student(STUDENT_ID, "pepito.perez@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"academic": {"studentCode": "506999999", "programCode": "506",
                                              "pensumCode": "1015", "currentLevel": 13}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("academic.currentLevel"));
    }

    @Test
    @DisplayName("an identification number shorter than 5 characters is rejected with 400")
    void patchMe_rejectsIdentificationTooShort() throws Exception {
        save(student(STUDENT_ID, "pepito.perez@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identification": {"type": "CC", "number": "1234"}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("identification.number"));
    }
}
