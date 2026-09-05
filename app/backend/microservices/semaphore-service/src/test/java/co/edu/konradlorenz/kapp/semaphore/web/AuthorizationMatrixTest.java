package co.edu.konradlorenz.kapp.semaphore.web;

import co.edu.konradlorenz.kapp.semaphore.client.UserProfileClient;
import co.edu.konradlorenz.kapp.semaphore.client.UserProfileResponse;
import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumStatus;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumAreaDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The exhaustive authorization matrix for every endpoint this service publishes,
 * against every role and against no token at all.
 *
 * <p>Every published rule in {@code docs/api/semaphore.openapi.yaml}'s "Authorization"
 * table gets its own test method per (endpoint, caller) pair - {@code ROLE_GUEST} denied
 * per endpoint, not asserted once and assumed to generalise; a wrong-but-authenticated
 * role refused per endpoint; the allowed role(s) actually let through. Each method makes
 * exactly one assertion, so a regression names the exact broken combination instead of
 * one test failing for an ambiguous reason.
 *
 * <p>Write endpoints always get a body that is valid under {@code @Valid} bean
 * validation, even in cases expected to be refused: {@code @RequestBody} argument
 * resolution runs before the {@code @PreAuthorize} advice fires (argument binding
 * happens while Spring MVC resolves the handler method's parameters, before the
 * AOP-proxied controller bean is actually invoked), so an invalid body would return
 * {@code 400} regardless of the caller's role and mask the authorization outcome this
 * class exists to prove.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class AuthorizationMatrixTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private UserProfileClient userProfileClient;

    private static final String SEEDED_PENSUM = "1015";
    private static final String SEEDED_PROGRAM = "506";
    private static final String SEEDED_COURSE_CODE = "10011";
    private static final String SEEDED_ELECTIVE = "ELECTIVA_VI";

    // ==================================================================================
    // GET /api/catalog/programs - any authenticated role except GUEST; ADMIN not required
    // ==================================================================================

    @Test
    @DisplayName("listPrograms: anonymous is refused with 401")
    void listPrograms_anonymous_401() throws Exception {
        mockMvc.perform(get("/api/catalog/programs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("listPrograms: GUEST is refused with 403")
    void listPrograms_guest_403() throws Exception {
        mockMvc.perform(get("/api/catalog/programs").with(guest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("listPrograms: STUDENT is allowed")
    void listPrograms_student_200() throws Exception {
        mockMvc.perform(get("/api/catalog/programs").with(student("s1")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("listPrograms: PROFESSOR is allowed")
    void listPrograms_professor_200() throws Exception {
        mockMvc.perform(get("/api/catalog/programs").with(professor("p1")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("listPrograms: ADMIN is allowed")
    void listPrograms_admin_200() throws Exception {
        mockMvc.perform(get("/api/catalog/programs").with(admin("a1")))
                .andExpect(status().isOk());
    }

    // ==================================================================================
    // GET /api/catalog/programs/{programCode}
    // ==================================================================================

    @Test
    @DisplayName("getProgram: anonymous is refused with 401")
    void getProgram_anonymous_401() throws Exception {
        mockMvc.perform(get("/api/catalog/programs/{code}", SEEDED_PROGRAM))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getProgram: GUEST is refused with 403")
    void getProgram_guest_403() throws Exception {
        mockMvc.perform(get("/api/catalog/programs/{code}", SEEDED_PROGRAM).with(guest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getProgram: STUDENT is allowed")
    void getProgram_student_200() throws Exception {
        mockMvc.perform(get("/api/catalog/programs/{code}", SEEDED_PROGRAM).with(student("s2")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getProgram: PROFESSOR is allowed")
    void getProgram_professor_200() throws Exception {
        mockMvc.perform(get("/api/catalog/programs/{code}", SEEDED_PROGRAM).with(professor("p2")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getProgram: ADMIN is allowed")
    void getProgram_admin_200() throws Exception {
        mockMvc.perform(get("/api/catalog/programs/{code}", SEEDED_PROGRAM).with(admin("a2")))
                .andExpect(status().isOk());
    }

    // ==================================================================================
    // GET /api/catalog/curricula/{pensumCode}
    // ==================================================================================

    @Test
    @DisplayName("getCurriculum: anonymous is refused with 401")
    void getCurriculum_anonymous_401() throws Exception {
        mockMvc.perform(get("/api/catalog/curricula/{code}", SEEDED_PENSUM))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getCurriculum: GUEST is refused with 403")
    void getCurriculum_guest_403() throws Exception {
        mockMvc.perform(get("/api/catalog/curricula/{code}", SEEDED_PENSUM).with(guest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getCurriculum: STUDENT is allowed")
    void getCurriculum_student_200() throws Exception {
        mockMvc.perform(get("/api/catalog/curricula/{code}", SEEDED_PENSUM).with(student("s3")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getCurriculum: PROFESSOR is allowed")
    void getCurriculum_professor_200() throws Exception {
        mockMvc.perform(get("/api/catalog/curricula/{code}", SEEDED_PENSUM).with(professor("p3")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getCurriculum: ADMIN is allowed")
    void getCurriculum_admin_200() throws Exception {
        mockMvc.perform(get("/api/catalog/curricula/{code}", SEEDED_PENSUM).with(admin("a3")))
                .andExpect(status().isOk());
    }

    // ==================================================================================
    // GET /api/catalog/curricula/{pensumCode}/courses
    // ==================================================================================

    @Test
    @DisplayName("listCurriculumCourses: anonymous is refused with 401")
    void listCurriculumCourses_anonymous_401() throws Exception {
        mockMvc.perform(get("/api/catalog/curricula/{code}/courses", SEEDED_PENSUM))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("listCurriculumCourses: GUEST is refused with 403")
    void listCurriculumCourses_guest_403() throws Exception {
        mockMvc.perform(get("/api/catalog/curricula/{code}/courses", SEEDED_PENSUM).with(guest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("listCurriculumCourses: STUDENT is allowed")
    void listCurriculumCourses_student_200() throws Exception {
        mockMvc.perform(get("/api/catalog/curricula/{code}/courses", SEEDED_PENSUM).with(student("s4")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("listCurriculumCourses: PROFESSOR is allowed")
    void listCurriculumCourses_professor_200() throws Exception {
        mockMvc.perform(get("/api/catalog/curricula/{code}/courses", SEEDED_PENSUM).with(professor("p4")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("listCurriculumCourses: ADMIN is allowed")
    void listCurriculumCourses_admin_200() throws Exception {
        mockMvc.perform(get("/api/catalog/curricula/{code}/courses", SEEDED_PENSUM).with(admin("a4")))
                .andExpect(status().isOk());
    }

    // ==================================================================================
    // POST /api/catalog/programs - ADMIN only
    // ==================================================================================

    @Test
    @DisplayName("createProgram: anonymous is refused with 401")
    void createProgram_anonymous_401() throws Exception {
        mockMvc.perform(post("/api/catalog/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProgramJson("MX-P-ANON")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("createProgram: GUEST is refused with 403")
    void createProgram_guest_403() throws Exception {
        mockMvc.perform(post("/api/catalog/programs").with(guest())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProgramJson("MX-P-GUEST")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("createProgram: STUDENT is refused with 403")
    void createProgram_student_403() throws Exception {
        mockMvc.perform(post("/api/catalog/programs").with(student("s9"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProgramJson("MX-P-STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("createProgram: PROFESSOR is refused with 403")
    void createProgram_professor_403() throws Exception {
        mockMvc.perform(post("/api/catalog/programs").with(professor("p9"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProgramJson("MX-P-PROFESSOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("createProgram: ADMIN is allowed")
    void createProgram_admin_201() throws Exception {
        mockMvc.perform(post("/api/catalog/programs").with(admin("a9"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProgramJson("MX-P-ADMIN")))
                .andExpect(status().isCreated());
    }

    // ==================================================================================
    // PUT /api/catalog/programs/{programCode} - ADMIN only
    // ==================================================================================

    @Test
    @DisplayName("replaceProgram: anonymous is refused with 401")
    void replaceProgram_anonymous_401() throws Exception {
        mockMvc.perform(put("/api/catalog/programs/{code}", "MX-PR-ANON")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProgramJson("MX-PR-ANON")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("replaceProgram: GUEST is refused with 403")
    void replaceProgram_guest_403() throws Exception {
        mockMvc.perform(put("/api/catalog/programs/{code}", "MX-PR-GUEST").with(guest())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProgramJson("MX-PR-GUEST")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("replaceProgram: STUDENT is refused with 403")
    void replaceProgram_student_403() throws Exception {
        mockMvc.perform(put("/api/catalog/programs/{code}", "MX-PR-STUDENT").with(student("s10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProgramJson("MX-PR-STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("replaceProgram: PROFESSOR is refused with 403")
    void replaceProgram_professor_403() throws Exception {
        mockMvc.perform(put("/api/catalog/programs/{code}", "MX-PR-PROF").with(professor("p10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProgramJson("MX-PR-PROF")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("replaceProgram: ADMIN is allowed (404 for an unknown code proves the gate passed)")
    void replaceProgram_admin_404() throws Exception {
        mockMvc.perform(put("/api/catalog/programs/{code}", "MX-PR-ADMIN").with(admin("a10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProgramJson("MX-PR-ADMIN")))
                .andExpect(status().isNotFound());
    }

    // ==================================================================================
    // DELETE /api/catalog/programs/{programCode} - ADMIN only
    // ==================================================================================

    @Test
    @DisplayName("deleteProgram: anonymous is refused with 401")
    void deleteProgram_anonymous_401() throws Exception {
        mockMvc.perform(delete("/api/catalog/programs/{code}", "MX-PD-ANON"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("deleteProgram: GUEST is refused with 403")
    void deleteProgram_guest_403() throws Exception {
        mockMvc.perform(delete("/api/catalog/programs/{code}", "MX-PD-GUEST").with(guest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleteProgram: STUDENT is refused with 403")
    void deleteProgram_student_403() throws Exception {
        mockMvc.perform(delete("/api/catalog/programs/{code}", "MX-PD-STUDENT").with(student("s11")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleteProgram: PROFESSOR is refused with 403")
    void deleteProgram_professor_403() throws Exception {
        mockMvc.perform(delete("/api/catalog/programs/{code}", "MX-PD-PROF").with(professor("p11")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleteProgram: ADMIN is allowed (404 for an unknown code proves the gate passed)")
    void deleteProgram_admin_404() throws Exception {
        mockMvc.perform(delete("/api/catalog/programs/{code}", "MX-PD-ADMIN").with(admin("a11")))
                .andExpect(status().isNotFound());
    }

    // ==================================================================================
    // DELETE /api/catalog/curricula/{pensumCode} - ADMIN only
    // ==================================================================================

    @Test
    @DisplayName("deleteCurriculum: anonymous is refused with 401")
    void deleteCurriculum_anonymous_401() throws Exception {
        mockMvc.perform(delete("/api/catalog/curricula/{code}", "MX-CD-ANON"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("deleteCurriculum: GUEST is refused with 403")
    void deleteCurriculum_guest_403() throws Exception {
        mockMvc.perform(delete("/api/catalog/curricula/{code}", "MX-CD-GUEST").with(guest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleteCurriculum: STUDENT is refused with 403")
    void deleteCurriculum_student_403() throws Exception {
        mockMvc.perform(delete("/api/catalog/curricula/{code}", "MX-CD-STUDENT").with(student("s12")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleteCurriculum: PROFESSOR is refused with 403")
    void deleteCurriculum_professor_403() throws Exception {
        mockMvc.perform(delete("/api/catalog/curricula/{code}", "MX-CD-PROF").with(professor("p12")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleteCurriculum: ADMIN is allowed (404 for an unknown code proves the gate passed)")
    void deleteCurriculum_admin_404() throws Exception {
        mockMvc.perform(delete("/api/catalog/curricula/{code}", "MX-CD-ADMIN").with(admin("a12")))
                .andExpect(status().isNotFound());
    }

    // ==================================================================================
    // POST /api/catalog/curricula - ADMIN only
    // ==================================================================================

    @Test
    @DisplayName("createCurriculum: anonymous is refused with 401")
    void createCurriculum_anonymous_401() throws Exception {
        mockMvc.perform(post("/api/catalog/curricula")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCurriculumJson("MATRIX-ANON")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("createCurriculum: GUEST is refused with 403")
    void createCurriculum_guest_403() throws Exception {
        mockMvc.perform(post("/api/catalog/curricula").with(guest())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCurriculumJson("MATRIX-GUEST")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("createCurriculum: STUDENT is refused with 403")
    void createCurriculum_student_403() throws Exception {
        mockMvc.perform(post("/api/catalog/curricula").with(student("s5"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCurriculumJson("MATRIX-STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("createCurriculum: PROFESSOR is refused with 403")
    void createCurriculum_professor_403() throws Exception {
        mockMvc.perform(post("/api/catalog/curricula").with(professor("p5"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCurriculumJson("MATRIX-PROFESSOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("createCurriculum: ADMIN is allowed")
    void createCurriculum_admin_201() throws Exception {
        mockMvc.perform(post("/api/catalog/curricula").with(admin("a5"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCurriculumJson("MATRIX-ADMIN-CREATE")))
                .andExpect(status().isCreated());
    }

    // ==================================================================================
    // PUT /api/catalog/curricula/{pensumCode} - ADMIN only
    // ==================================================================================

    @Test
    @DisplayName("replaceCurriculum: anonymous is refused with 401")
    void replaceCurriculum_anonymous_401() throws Exception {
        mockMvc.perform(put("/api/catalog/curricula/{code}", "MX-REPL-ANON")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCurriculumJson("MX-REPL-ANON")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("replaceCurriculum: GUEST is refused with 403")
    void replaceCurriculum_guest_403() throws Exception {
        mockMvc.perform(put("/api/catalog/curricula/{code}", "MX-REPL-GUEST").with(guest())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCurriculumJson("MX-REPL-GUEST")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("replaceCurriculum: STUDENT is refused with 403")
    void replaceCurriculum_student_403() throws Exception {
        mockMvc.perform(put("/api/catalog/curricula/{code}", "MX-REPL-STUDENT").with(student("s6"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCurriculumJson("MX-REPL-STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("replaceCurriculum: PROFESSOR is refused with 403")
    void replaceCurriculum_professor_403() throws Exception {
        mockMvc.perform(put("/api/catalog/curricula/{code}", "MX-REPL-PROF").with(professor("p6"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCurriculumJson("MX-REPL-PROF")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("replaceCurriculum: ADMIN is allowed")
    void replaceCurriculum_admin_200() throws Exception {
        String pensumCode = "MX-REPL-ADMIN";
        // Setup: the curriculum must already exist for a replace to succeed.
        mockMvc.perform(post("/api/catalog/curricula").with(admin("a6-setup"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCurriculumJson(pensumCode)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/catalog/curricula/{code}", pensumCode).with(admin("a6"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCurriculumJson(pensumCode)))
                .andExpect(status().isOk());
    }

    // ==================================================================================
    // /api/semaphore/me/** - ROLE_STUDENT only, nobody else (not even ADMIN or PROFESSOR)
    // ==================================================================================

    @Test
    @DisplayName("getMine: anonymous is refused with 401")
    void getMine_anonymous_401() throws Exception {
        mockMvc.perform(get("/api/semaphore/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getMine: GUEST is refused with 403")
    void getMine_guest_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/me").with(guest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getMine: PROFESSOR is refused with 403")
    void getMine_professor_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/me").with(professor("p7")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getMine: ADMIN is refused with 403 - an admin reads /api/semaphore/{userId} instead")
    void getMine_admin_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/me").with(admin("a7")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getMine: STUDENT is allowed and lazily creates the document")
    void getMine_student_200() throws Exception {
        stubActiveStudentProfile();
        mockMvc.perform(get("/api/semaphore/me").with(student("matrix-me-student")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getSummary: anonymous is refused with 401")
    void getSummary_anonymous_401() throws Exception {
        mockMvc.perform(get("/api/semaphore/me/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getSummary: GUEST is refused with 403")
    void getSummary_guest_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/me/summary").with(guest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getSummary: PROFESSOR is refused with 403")
    void getSummary_professor_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/me/summary").with(professor("p8")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getSummary: ADMIN is refused with 403")
    void getSummary_admin_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/me/summary").with(admin("a8")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getSummary: STUDENT is allowed")
    void getSummary_student_200() throws Exception {
        stubActiveStudentProfile();
        mockMvc.perform(get("/api/semaphore/me/summary").with(student("matrix-summary-student")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getEligible: anonymous is refused with 401")
    void getEligible_anonymous_401() throws Exception {
        mockMvc.perform(get("/api/semaphore/me/eligible"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getEligible: GUEST is refused with 403")
    void getEligible_guest_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/me/eligible").with(guest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getEligible: PROFESSOR is refused with 403")
    void getEligible_professor_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/me/eligible").with(professor("p9")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getEligible: ADMIN is refused with 403")
    void getEligible_admin_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/me/eligible").with(admin("a9")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getEligible: STUDENT is allowed")
    void getEligible_student_200() throws Exception {
        stubActiveStudentProfile();
        mockMvc.perform(get("/api/semaphore/me/eligible").with(student("matrix-eligible-student")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("updateCourseStatus: anonymous is refused with 401")
    void updateCourseStatus_anonymous_401() throws Exception {
        mockMvc.perform(put("/api/semaphore/me/courses/{code}", SEEDED_COURSE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCourseUpdateJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("updateCourseStatus: GUEST is refused with 403")
    void updateCourseStatus_guest_403() throws Exception {
        mockMvc.perform(put("/api/semaphore/me/courses/{code}", SEEDED_COURSE_CODE).with(guest())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCourseUpdateJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("updateCourseStatus: PROFESSOR is refused with 403")
    void updateCourseStatus_professor_403() throws Exception {
        mockMvc.perform(put("/api/semaphore/me/courses/{code}", SEEDED_COURSE_CODE).with(professor("p10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCourseUpdateJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("updateCourseStatus: ADMIN is refused with 403")
    void updateCourseStatus_admin_403() throws Exception {
        mockMvc.perform(put("/api/semaphore/me/courses/{code}", SEEDED_COURSE_CODE).with(admin("a10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCourseUpdateJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("updateCourseStatus: STUDENT is allowed")
    void updateCourseStatus_student_200() throws Exception {
        stubActiveStudentProfile();
        mockMvc.perform(put("/api/semaphore/me/courses/{code}", SEEDED_COURSE_CODE)
                        .with(student("matrix-update-student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCourseUpdateJson()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("resolveElective: anonymous is refused with 401")
    void resolveElective_anonymous_401() throws Exception {
        mockMvc.perform(post("/api/semaphore/me/electives/{code}", SEEDED_ELECTIVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validElectiveResolutionJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("resolveElective: GUEST is refused with 403")
    void resolveElective_guest_403() throws Exception {
        mockMvc.perform(post("/api/semaphore/me/electives/{code}", SEEDED_ELECTIVE).with(guest())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validElectiveResolutionJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("resolveElective: PROFESSOR is refused with 403")
    void resolveElective_professor_403() throws Exception {
        mockMvc.perform(post("/api/semaphore/me/electives/{code}", SEEDED_ELECTIVE).with(professor("p11"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validElectiveResolutionJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("resolveElective: ADMIN is refused with 403")
    void resolveElective_admin_403() throws Exception {
        mockMvc.perform(post("/api/semaphore/me/electives/{code}", SEEDED_ELECTIVE).with(admin("a11"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validElectiveResolutionJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("resolveElective: STUDENT is allowed")
    void resolveElective_student_200() throws Exception {
        stubActiveStudentProfile();
        mockMvc.perform(post("/api/semaphore/me/electives/{code}", SEEDED_ELECTIVE)
                        .with(student("matrix-resolve-student"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validElectiveResolutionJson()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("clearElective: anonymous is refused with 401")
    void clearElective_anonymous_401() throws Exception {
        mockMvc.perform(delete("/api/semaphore/me/electives/{code}", SEEDED_ELECTIVE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("clearElective: GUEST is refused with 403")
    void clearElective_guest_403() throws Exception {
        mockMvc.perform(delete("/api/semaphore/me/electives/{code}", SEEDED_ELECTIVE).with(guest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("clearElective: PROFESSOR is refused with 403")
    void clearElective_professor_403() throws Exception {
        mockMvc.perform(delete("/api/semaphore/me/electives/{code}", SEEDED_ELECTIVE).with(professor("p12")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("clearElective: ADMIN is refused with 403")
    void clearElective_admin_403() throws Exception {
        mockMvc.perform(delete("/api/semaphore/me/electives/{code}", SEEDED_ELECTIVE).with(admin("a12")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("clearElective: STUDENT is allowed")
    void clearElective_student_204() throws Exception {
        stubActiveStudentProfile();
        mockMvc.perform(delete("/api/semaphore/me/electives/{code}", SEEDED_ELECTIVE)
                        .with(student("matrix-clear-student")))
                .andExpect(status().isNoContent());
    }

    // ==================================================================================
    // GET /api/semaphore/{userId} - ADMIN only
    // ==================================================================================

    @Test
    @DisplayName("getStudentSemaphore: anonymous is refused with 401")
    void getStudentSemaphore_anonymous_401() throws Exception {
        mockMvc.perform(get("/api/semaphore/{userId}", "does-not-matter"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getStudentSemaphore: GUEST is refused with 403")
    void getStudentSemaphore_guest_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/{userId}", "does-not-matter").with(guest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getStudentSemaphore: STUDENT is refused with 403")
    void getStudentSemaphore_student_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/{userId}", "does-not-matter").with(student("s13")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getStudentSemaphore: PROFESSOR is refused with 403")
    void getStudentSemaphore_professor_403() throws Exception {
        mockMvc.perform(get("/api/semaphore/{userId}", "does-not-matter").with(professor("p13")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getStudentSemaphore: ADMIN is allowed (404 for an unknown user proves the gate passed)")
    void getStudentSemaphore_admin_404() throws Exception {
        mockMvc.perform(get("/api/semaphore/{userId}", "no-such-user-in-this-test").with(admin("a13")))
                .andExpect(status().isNotFound());
    }

    // ---- helpers ---------------------------------------------------------------------

    private void stubActiveStudentProfile() {
        UserProfileResponse profile = new UserProfileResponse("profile-id", "ROLE_STUDENT",
                new UserProfileResponse.Academic("506232730", SEEDED_PROGRAM, 1));
        lenient().when(userProfileClient.getMyProfile()).thenReturn(profile);
    }

    private String validCourseUpdateJson() throws Exception {
        return """
                {"status":"IN_PROGRESS","period":"20261"}""";
    }

    private String validElectiveResolutionJson() throws Exception {
        // A code from outside the seeded pensum, on purpose: electives are frequently
        // taken from other programs, and a code already on this pensum (e.g. "59035",
        // itself a fixed course elsewhere in 1015) would legitimately be rejected as
        // "already accounted for elsewhere on the semaforo".
        return """
                {"resolvedCode":"EXTERNAL-101","resolvedName":"Elective From Another Program"}""";
    }

    /**
     * A body that passes {@code @Valid} even in the cases expected to be refused. Argument
     * binding runs before the {@code @PreAuthorize} advice, so an invalid body would return
     * 400 regardless of the caller's role and mask the authorization outcome.
     */
    private String validProgramJson(String code) throws Exception {
        return mapper.writeValueAsString(new co.edu.konradlorenz.kapp.semaphore.web.dto.ProgramRequest(
                code, "Matrix Program", "Matrix Faculty",
                co.edu.konradlorenz.kapp.semaphore.domain.ProgramLevel.PREGRADO));
    }

    private String validCurriculumJson(String pensumCode) throws Exception {
        CurriculumAreaDto area = new CurriculumAreaDto("CB", "Ciencias Basicas", "#539392", 3, 4);
        // A short, fixed course code: unique only needs to hold within this one
        // document, not across every generated pensumCode, and course.code() is
        // capped at 20 chars while some test pensumCodes are already close to that.
        CurriculumCourseDto course = new CurriculumCourseDto(
                "X1", "X1", "Course for " + pensumCode, 1, 3, 4, null,
                "CB", false, List.of(), null);
        CurriculumDto dto = new CurriculumDto(pensumCode, SEEDED_PROGRAM, "Test Program",
                "Test Faculty", "Test Reform", CurriculumStatus.ACTIVE,
                3, 4, 1, List.of(area), List.of(course));
        return mapper.writeValueAsString(dto);
    }

    private static RequestPostProcessor guest() {
        return jwtWithRole("guest", "ROLE_GUEST");
    }

    private static RequestPostProcessor student(String subject) {
        return jwtWithRole(subject, "ROLE_STUDENT");
    }

    private static RequestPostProcessor professor(String subject) {
        return jwtWithRole(subject, "ROLE_PROFESSOR");
    }

    private static RequestPostProcessor admin(String subject) {
        return jwtWithRole(subject, "ROLE_ADMIN");
    }

    private static RequestPostProcessor jwtWithRole(String subject, String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(b -> b.subject(subject)
                        .claim("email", subject + "@konradlorenz.edu.co")
                        .claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority(role));
    }
}
