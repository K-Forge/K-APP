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

    // ---------------------------------------------------------------------------------
    // GET /api/users/me
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("a student's own profile carries the full academic record")
    void getMe_student_returnsAcademicRecord() throws Exception {
        save(fullyPopulated(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co"));

        mockMvc.perform(get("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(STUDENT_ID))
                .andExpect(jsonPath("$.email").value("brian.vargasc@konradlorenz.edu.co"))
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"))
                .andExpect(jsonPath("$.academic.studentCode").value("506232730"))
                .andExpect(jsonPath("$.academic.currentLevel").value(6));
    }

    @Test
    @DisplayName("a guest's profile round-trips with academic explicitly null, not absent")
    void getMe_guest_academicIsExplicitlyNull() throws Exception {
        save(guest(GUEST_ID, "maria.rodriguez@gmail.com", "Maria", "Rodriguez"));

        mockMvc.perform(get("/api/users/me").with(callerWith(GUEST_ID, UserRole.ROLE_GUEST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_GUEST"))
                .andExpect(jsonPath("$..academic").exists())
                .andExpect(jsonPath("$.academic").value(nullValue()));
    }

    // ---------------------------------------------------------------------------------
    // PATCH /api/users/me - partial update semantics
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("a field left out of the body is not touched")
    void patchMe_omittedField_isUnchanged() throws Exception {
        save(fullyPopulated(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Updated"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                // lastName was not in the body, so the stored value survives untouched.
                .andExpect(jsonPath("$.lastName").value("Vargas Clavijo"))
                .andExpect(jsonPath("$.phone").value("+573105551234"));
    }

    @Test
    @DisplayName("an explicit null clears the phone")
    void patchMe_explicitNullPhone_clearsIt() throws Exception {
        save(fullyPopulated(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": null}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(nullValue()))
                // Untouched fields, including the ones a null-collapsing bug would wipe.
                .andExpect(jsonPath("$.firstName").value("Brian Steven"));
    }

    @Test
    @DisplayName("an explicit null clears the identification")
    void patchMe_explicitNullIdentification_clearsIt() throws Exception {
        save(fullyPopulated(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co"));

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
        save(student(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co", "Brian", "Vargas"));

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
        save(student(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co", "Brian", "Vargas"));

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
        save(student(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co", "Brian", "Vargas"));

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
        save(student(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co", "Brian", "Vargas"));

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
        save(student(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co", "Brian", "Vargas"));

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
        save(student(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co", "Brian", "Vargas"));

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
        save(student(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------------------------
    // PATCH /api/users/me - the guest / academic business rule
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("a guest may not be given an academic record")
    void patchMe_rejectsAcademicOnGuest() throws Exception {
        save(guest(GUEST_ID, "maria.rodriguez@gmail.com", "Maria", "Rodriguez"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(GUEST_ID, UserRole.ROLE_GUEST))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"academic": {"studentCode": "506999999", "programCode": "506",
                                              "pensumCode": "1015", "currentLevel": 3}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("academic"));

        UserProfile stillAGuest = reload(GUEST_ID);
        org.assertj.core.api.Assertions.assertThat(stillAGuest.academic()).isNull();
    }

    // ---------------------------------------------------------------------------------
    // PATCH /api/users/me - field-level validation, matching the contract exactly
    // ---------------------------------------------------------------------------------

    @Test
    @DisplayName("a phone not in E.164 form is rejected with 400")
    void patchMe_rejectsMalformedPhone() throws Exception {
        save(student(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co", "Brian", "Vargas"));

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
        save(student(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co", "Brian", "Vargas"));

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
        save(student(STUDENT_ID, "brian.vargasc@konradlorenz.edu.co", "Brian", "Vargas"));

        mockMvc.perform(patch("/api/users/me").with(callerWith(STUDENT_ID, UserRole.ROLE_STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identification": {"type": "CC", "number": "1234"}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("identification.number"));
    }
}
