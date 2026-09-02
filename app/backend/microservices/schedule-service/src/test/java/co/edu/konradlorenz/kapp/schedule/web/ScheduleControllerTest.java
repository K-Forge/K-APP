package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.schedule.AbstractScheduleServiceTest;
import co.edu.konradlorenz.kapp.schedule.domain.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code /api/schedule/me} and {@code /api/schedule/me/periods}: creation, retrieval,
 * deletion and the period switcher. Who may call these at all is
 * {@link ScheduleAuthorizationMatrixTest}'s job; this class is about behaviour.
 */
class ScheduleControllerTest extends AbstractScheduleServiceTest {

    private static final String STUDENT_ID = "e0000000-0000-0000-0000-000000000001";
    private static final String OTHER_STUDENT_ID = "e0000000-0000-0000-0000-000000000002";

    @Test
    @DisplayName("GET /me with no period returns the active schedule")
    void getMe_noPeriod_returnsActiveSchedule() throws Exception {
        save(schedule(STUDENT_ID, "20261", false, List.of()));
        save(schedule(STUDENT_ID, "20262", true, List.of()));

        mockMvc.perform(get("/api/schedule/me").with(student(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("20262"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("GET /me?period=... returns that specific period, active or not")
    void getMe_explicitPeriod_returnsThatPeriod() throws Exception {
        save(schedule(STUDENT_ID, "20261", false, List.of()));
        save(schedule(STUDENT_ID, "20262", true, List.of()));

        mockMvc.perform(get("/api/schedule/me").param("period", "20261").with(student(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("20261"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("GET /me for a period the caller has no schedule for is a 404")
    void getMe_unknownPeriod_returns404() throws Exception {
        mockMvc.perform(get("/api/schedule/me").param("period", "20261").with(student(STUDENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /me with a malformed period is a 400, not a 500")
    void getMe_malformedPeriod_returns400() throws Exception {
        mockMvc.perform(get("/api/schedule/me").param("period", "2026-2").with(student(STUDENT_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a student only ever sees their own schedule through /me")
    void getMe_isIsolatedPerStudent() throws Exception {
        Schedule mine = save(schedule(STUDENT_ID, "20262", true, List.of()));
        save(schedule(OTHER_STUDENT_ID, "20262", true, List.of()));

        mockMvc.perform(get("/api/schedule/me").with(student(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mine.id()))
                .andExpect(jsonPath("$.userId").value(STUDENT_ID));
    }

    @Test
    @DisplayName("POST /me creates an empty, active schedule")
    void createMe_createsEmptyActiveSchedule() throws Exception {
        mockMvc.perform(post("/api/schedule/me").with(student(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"period": "20262", "programCode": "506", "pensumCode": "1015", "level": 8}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(STUDENT_ID))
                .andExpect(jsonPath("$.period").value("20262"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.enrollments").isEmpty());
    }

    @Test
    @DisplayName("creating a second schedule for a period already held is a 409")
    void createMe_duplicatePeriod_returns409() throws Exception {
        save(schedule(STUDENT_ID, "20262", true, List.of()));

        mockMvc.perform(post("/api/schedule/me").with(student(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"period": "20262", "programCode": "506", "pensumCode": "1015", "level": 8}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("a malformed period on create is a 400 naming the field")
    void createMe_malformedPeriod_returns400() throws Exception {
        mockMvc.perform(post("/api/schedule/me").with(student(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"period": "2026S2", "programCode": "506", "pensumCode": "1015", "level": 8}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("period"));
    }

    @Test
    @DisplayName("creating a new schedule demotes the previously active one")
    void createMe_demotesThePreviouslyActiveSchedule() throws Exception {
        Schedule previous = save(schedule(STUDENT_ID, "20261", true, List.of()));

        mockMvc.perform(post("/api/schedule/me").with(student(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"period": "20262", "programCode": "506", "pensumCode": "1015", "level": 8}"""))
                .andExpect(status().isCreated());

        Schedule reloaded = reload(previous.id());
        org.assertj.core.api.Assertions.assertThat(reloaded.active()).isFalse();
    }

    @Test
    @DisplayName("DELETE /me removes the schedule for the given period")
    void deleteMe_removesSchedule() throws Exception {
        Schedule toDelete = save(schedule(STUDENT_ID, "20262", true, List.of()));

        mockMvc.perform(delete("/api/schedule/me").param("period", "20262").with(student(STUDENT_ID)))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(reload(toDelete.id())).isNull();
    }

    @Test
    @DisplayName("DELETE /me without a period is a 400, not a 500")
    void deleteMe_missingPeriod_returns400() throws Exception {
        mockMvc.perform(delete("/api/schedule/me").with(student(STUDENT_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /me for a period the caller has no schedule for is a 404")
    void deleteMe_unknownPeriod_returns404() throws Exception {
        mockMvc.perform(delete("/api/schedule/me").param("period", "20262").with(student(STUDENT_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /me/periods lists newest period first, with enrollment counts")
    void listPeriods_newestFirstWithCounts() throws Exception {
        save(schedule(STUDENT_ID, "20252", false, List.of()));
        save(schedule(STUDENT_ID, "20261", false, List.of(estadistica(List.of()))));
        save(schedule(STUDENT_ID, "20262", true, List.of()));

        mockMvc.perform(get("/api/schedule/me/periods").with(student(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].period").value("20262"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].period").value("20261"))
                .andExpect(jsonPath("$[1].enrollmentCount").value(1))
                .andExpect(jsonPath("$[2].period").value("20252"));
    }
}
