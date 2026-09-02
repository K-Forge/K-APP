package co.edu.konradlorenz.kapp.schedule.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Wire shape of {@code Enrollment}. Built only by {@code mapper.ScheduleMapper}. */
public record EnrollmentResponse(
        String enrollmentId,
        String courseCode,
        String pensumItemCode,
        String courseName,
        Integer level,
        Integer credits,
        Integer totalHours,
        String group,
        String subgroup,
        String professor,
        String campus,
        LocalDate startDate,
        LocalDate endDate,
        String color,
        List<MeetingResponse> meetings
) {
}
