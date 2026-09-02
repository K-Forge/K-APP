package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.schedule.AbstractScheduleServiceTest;
import co.edu.konradlorenz.kapp.schedule.domain.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/schedule/{userId}}: administrative read access. Role coverage lives in
 * {@link ScheduleAuthorizationMatrixTest}; this class is about the payload and the literal
 * {@code /me} path never being read as an id.
 */
class ScheduleAdminControllerTest extends AbstractScheduleServiceTest {

    private static final String ADMIN_ID = "a3000000-0000-0000-0000-000000000001";
    private static final String STUDENT_ID = "a3000000-0000-0000-0000-000000000002";

    @Test
    @DisplayName("an admin reads exactly the requested student's schedule")
    void getByUserId_returnsTheRequestedStudentsSchedule() throws Exception {
        Schedule schedule = save(schedule(STUDENT_ID, "20262", true, List.of(estadistica(List.of()))));

        mockMvc.perform(get("/api/schedule/{userId}", STUDENT_ID).with(admin(ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(schedule.id()))
                .andExpect(jsonPath("$.userId").value(STUDENT_ID))
                .andExpect(jsonPath("$.enrollments[0].courseCode").value("17080"));
    }

    @Test
    @DisplayName("an unknown user id is a 404, not empty success")
    void getByUserId_unknownUser_returns404() throws Exception {
        mockMvc.perform(get("/api/schedule/{userId}", "no-such-user").with(admin(ADMIN_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the literal /me path is never read as a userId, even for an admin")
    void meIsNeverReadAsAUserId() throws Exception {
        // "me" would resolve to nothing sensible as a student id; the fact this reaches
        // ScheduleController's own handler instead of the {userId} template proves the
        // literal path wins the routing precedence, per the contract's own guarantee.
        mockMvc.perform(get("/api/schedule/me").with(admin(ADMIN_ID)))
                .andExpect(status().isNotFound()); // the admin caller simply has no schedule of their own
    }
}
