package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.schedule.AbstractScheduleServiceTest;
import co.edu.konradlorenz.kapp.schedule.domain.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code /api/schedule/me/day} and {@code /api/schedule/me/week}: the resolved views that
 * make the schedule → enrollment → meeting → meeting-period nesting worth it.
 */
class AgendaControllerTest extends AbstractScheduleServiceTest {

    private static final String STUDENT_ID = "a2000000-0000-0000-0000-000000000001";

    private Schedule seedEstadistica() {
        return schedule(STUDENT_ID, "20262", true, List.of(estadistica(List.of(
                meeting(DayOfWeek.MONDAY, "18:15", "20:30",
                        period("2026-07-27", "2026-08-10", "302"),
                        period("2026-08-24", "2026-09-14", "302"),
                        period("2026-09-21", "2026-09-28", null),
                        period("2026-10-19", "2026-10-26", "302"))))));
    }

    @Test
    @DisplayName("a date inside a range returns the class with the room in force")
    void day_dateInsideRange_returnsClassWithRoom() throws Exception {
        save(seedEstadistica());

        mockMvc.perform(get("/api/schedule/me/day").param("date", "2026-08-24").with(student(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseCode").value("17080"))
                .andExpect(jsonPath("$[0].startTime").value("18:15"))
                .andExpect(jsonPath("$[0].room").value("302"));
    }

    @Test
    @DisplayName("a date inside the roomless range returns the class with a null room")
    void day_dateInRoomlessRange_returnsNullRoom() throws Exception {
        save(seedEstadistica());

        mockMvc.perform(get("/api/schedule/me/day").param("date", "2026-09-21").with(student(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].room").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("a date in the gap between two ranges returns no classes")
    void day_dateInGap_returnsEmpty() throws Exception {
        save(seedEstadistica());

        // 2026-08-17 is the Monday between the 2026-08-10 and 2026-08-24 ranges.
        mockMvc.perform(get("/api/schedule/me/day").param("date", "2026-08-17").with(student(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("day with no active schedule is a 404")
    void day_noActiveSchedule_returns404() throws Exception {
        mockMvc.perform(get("/api/schedule/me/day").param("date", "2026-08-24").with(student(STUDENT_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("day without a date is a 400, not a 500")
    void day_missingDate_returns400() throws Exception {
        save(seedEstadistica());

        mockMvc.perform(get("/api/schedule/me/day").with(student(STUDENT_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("week groups the resolved classes by day, all seven keys present")
    void week_groupsByDayWithAllSevenKeys() throws Exception {
        save(seedEstadistica());

        // 2026-08-24 is the Monday of that week; the class occurs that day only.
        mockMvc.perform(get("/api/schedule/me/week").param("date", "2026-08-24").with(student(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-08-24"))
                .andExpect(jsonPath("$.weekEnd").value("2026-08-30"))
                .andExpect(jsonPath("$.days.MONDAY.length()").value(1))
                .andExpect(jsonPath("$.days.MONDAY[0].room").value("302"))
                .andExpect(jsonPath("$.days.TUESDAY.length()").value(0))
                .andExpect(jsonPath("$.days.WEDNESDAY.length()").value(0))
                .andExpect(jsonPath("$.days.THURSDAY.length()").value(0))
                .andExpect(jsonPath("$.days.FRIDAY.length()").value(0))
                .andExpect(jsonPath("$.days.SATURDAY.length()").value(0))
                .andExpect(jsonPath("$.days.SUNDAY.length()").value(0));
    }

    @Test
    @DisplayName("week snaps any date inside the week to that week's Monday")
    void week_snapsToMonday() throws Exception {
        save(seedEstadistica());

        // 2026-08-27 is the Thursday of the same week as 2026-08-24.
        mockMvc.perform(get("/api/schedule/me/week").param("date", "2026-08-27").with(student(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-08-24"));
    }

    @Test
    @DisplayName("week for a week the meeting has a gap in returns an empty Monday")
    void week_weekInGap_returnsEmptyMonday() throws Exception {
        save(seedEstadistica());

        mockMvc.perform(get("/api/schedule/me/week").param("date", "2026-08-17").with(student(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.MONDAY.length()").value(0));
    }

    @Test
    @DisplayName("week with no active schedule is a 404")
    void week_noActiveSchedule_returns404() throws Exception {
        mockMvc.perform(get("/api/schedule/me/week").param("date", "2026-08-24").with(student(STUDENT_ID)))
                .andExpect(status().isNotFound());
    }
}
