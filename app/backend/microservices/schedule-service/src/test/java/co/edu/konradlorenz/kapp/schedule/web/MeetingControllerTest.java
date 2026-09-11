package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.schedule.AbstractScheduleServiceTest;
import co.edu.konradlorenz.kapp.schedule.domain.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.DayOfWeek;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code .../enrollments/{enrollmentId}/meetings}: weekly slots within one enrollment. */
class MeetingControllerTest extends AbstractScheduleServiceTest {

    private static final String STUDENT_ID = "a1000000-0000-0000-0000-000000000001";

    @Test
    @DisplayName("adding a meeting whose time touches an existing one at the boundary succeeds")
    void addMeeting_touchingAtBoundary_succeeds() throws Exception {
        Schedule seeded = save(schedule(STUDENT_ID, "20262", true, List.of(estadistica(List.of(
                meeting(DayOfWeek.MONDAY, "18:15", "20:30", period("2026-07-27", "2026-11-30", "302")))))));
        String enrollmentId = seeded.enrollments().get(0).enrollmentId();

        // Starts exactly when the seeded meeting ends: half-open [start, end) means this
        // does not conflict, even on the same day and overlapping dates.
        String body = """
                {"dayOfWeek": "MONDAY", "startTime": "20:30", "endTime": "21:30",
                 "periods": [{"from": "2026-07-27", "to": "2026-11-30", "room": "610"}]}""";

        mockMvc.perform(post("/api/schedule/me/enrollments/{id}/meetings", enrollmentId)
                        .with(student(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").value("20:30"));
    }

    @Test
    @DisplayName("adding a meeting that overlaps an existing one is a 409")
    void addMeeting_overlapping_returns409() throws Exception {
        Schedule seeded = save(schedule(STUDENT_ID, "20262", true, List.of(estadistica(List.of(
                meeting(DayOfWeek.MONDAY, "18:15", "20:30", period("2026-07-27", "2026-11-30", "302")))))));
        String enrollmentId = seeded.enrollments().get(0).enrollmentId();

        String body = """
                {"dayOfWeek": "MONDAY", "startTime": "19:00", "endTime": "21:00",
                 "periods": [{"from": "2026-07-27", "to": "2026-11-30", "room": "610"}]}""";

        mockMvc.perform(post("/api/schedule/me/enrollments/{id}/meetings", enrollmentId)
                        .with(student(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("adding a meeting to an enrollment that does not exist is a 404")
    void addMeeting_unknownEnrollment_returns404() throws Exception {
        save(schedule(STUDENT_ID, "20262", true, List.of()));

        String body = """
                {"dayOfWeek": "MONDAY", "startTime": "19:00", "endTime": "21:00",
                 "periods": [{"from": "2026-07-27", "to": "2026-11-30", "room": "610"}]}""";

        mockMvc.perform(post("/api/schedule/me/enrollments/{id}/meetings", "does-not-exist")
                        .with(student(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("periods sent on PATCH replace the whole list, not merge into it")
    void updateMeeting_periodsReplaceWholesale() throws Exception {
        Schedule seeded = save(schedule(STUDENT_ID, "20262", true, List.of(estadistica(List.of(
                meeting(DayOfWeek.MONDAY, "18:15", "20:30",
                        period("2026-07-27", "2026-08-10", "302"),
                        period("2026-08-24", "2026-09-14", "302")))))));
        String enrollmentId = seeded.enrollments().get(0).enrollmentId();
        String meetingId = seeded.enrollments().get(0).meetings().get(0).meetingId();

        String body = """
                {"periods": [{"from": "2026-10-19", "to": "2026-10-26", "room": "612"}]}""";

        mockMvc.perform(patch("/api/schedule/me/enrollments/{eid}/meetings/{mid}", enrollmentId, meetingId)
                        .with(student(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periods.length()").value(1))
                .andExpect(jsonPath("$.periods[0].from").value("2026-10-19"))
                .andExpect(jsonPath("$.periods[0].room").value("612"));
    }

    @Test
    @DisplayName("updating a meeting is not compared against itself")
    void updateMeeting_notComparedAgainstItself() throws Exception {
        Schedule seeded = save(schedule(STUDENT_ID, "20262", true, List.of(estadistica(List.of(
                meeting(DayOfWeek.MONDAY, "18:15", "20:30", period("2026-07-27", "2026-11-30", "302")))))));
        String enrollmentId = seeded.enrollments().get(0).enrollmentId();
        String meetingId = seeded.enrollments().get(0).meetings().get(0).meetingId();

        // Same day, same dates, five minutes earlier: would conflict with any other
        // meeting, but this is the meeting patching itself, so it must succeed.
        mockMvc.perform(patch("/api/schedule/me/enrollments/{eid}/meetings/{mid}", enrollmentId, meetingId)
                        .with(student(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"startTime": "18:10"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("18:10"));
    }

    @Test
    @DisplayName("updating a meeting into a conflict with a sibling meeting is a 409")
    void updateMeeting_intoConflictWithSibling_returns409() throws Exception {
        Schedule seeded = save(schedule(STUDENT_ID, "20262", true, List.of(estadistica(List.of(
                meeting(DayOfWeek.MONDAY, "18:15", "20:30", period("2026-07-27", "2026-11-30", "302")),
                meeting(DayOfWeek.WEDNESDAY, "18:15", "20:30", period("2026-07-27", "2026-11-30", "302")))))));
        String enrollmentId = seeded.enrollments().get(0).enrollmentId();
        String wednesdayMeetingId = seeded.enrollments().get(0).meetings().get(1).meetingId();

        mockMvc.perform(patch("/api/schedule/me/enrollments/{eid}/meetings/{mid}", enrollmentId,
                        wednesdayMeetingId)
                        .with(student(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"dayOfWeek": "MONDAY"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("deleting a meeting keeps the enrollment even when it was the last meeting")
    void deleteMeeting_keepsTheEnrollment() throws Exception {
        Schedule seeded = save(schedule(STUDENT_ID, "20262", true, List.of(estadistica(List.of(
                meeting(DayOfWeek.MONDAY, "18:15", "20:30", period("2026-07-27", "2026-11-30", "302")))))));
        String enrollmentId = seeded.enrollments().get(0).enrollmentId();
        String meetingId = seeded.enrollments().get(0).meetings().get(0).meetingId();

        mockMvc.perform(delete("/api/schedule/me/enrollments/{eid}/meetings/{mid}", enrollmentId, meetingId)
                        .with(student(STUDENT_ID)))
                .andExpect(status().isNoContent());

        Schedule reloaded = reload(seeded.id());
        assertThat(reloaded.enrollments()).hasSize(1);
        assertThat(reloaded.enrollments().get(0).meetings()).isEmpty();
    }

    @Test
    @DisplayName("deleting a meeting that does not exist is a 404")
    void deleteMeeting_unknownMeeting_returns404() throws Exception {
        Schedule seeded = save(schedule(STUDENT_ID, "20262", true, List.of(estadistica(List.of()))));
        String enrollmentId = seeded.enrollments().get(0).enrollmentId();

        mockMvc.perform(delete("/api/schedule/me/enrollments/{eid}/meetings/{mid}", enrollmentId, "does-not-exist")
                        .with(student(STUDENT_ID)))
                .andExpect(status().isNotFound());
    }
}
