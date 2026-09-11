package co.edu.konradlorenz.kapp.schedule.mapper;

import co.edu.konradlorenz.kapp.schedule.domain.Enrollment;
import co.edu.konradlorenz.kapp.schedule.domain.Meeting;
import co.edu.konradlorenz.kapp.schedule.domain.MeetingPeriod;
import co.edu.konradlorenz.kapp.schedule.domain.Schedule;
import co.edu.konradlorenz.kapp.schedule.web.dto.ClassOccurrenceResponse;
import co.edu.konradlorenz.kapp.schedule.web.dto.EnrollmentResponse;
import co.edu.konradlorenz.kapp.schedule.web.dto.MeetingPeriodResponse;
import co.edu.konradlorenz.kapp.schedule.web.dto.MeetingResponse;
import co.edu.konradlorenz.kapp.schedule.web.dto.ScheduleResponse;

/**
 * The single place a domain object turns into the shape the API returns. Every controller
 * and every test goes through here, so the wire format only has to be gotten right once.
 */
public final class ScheduleMapper {

    private ScheduleMapper() {
    }

    public static ScheduleResponse toResponse(Schedule schedule) {
        return new ScheduleResponse(
                schedule.id(),
                schedule.userId(),
                schedule.period(),
                schedule.programCode(),
                schedule.pensumCode(),
                schedule.level(),
                schedule.active(),
                schedule.enrollments().stream().map(ScheduleMapper::toResponse).toList());
    }

    public static EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.enrollmentId(),
                enrollment.courseCode(),
                enrollment.pensumItemCode(),
                enrollment.courseName(),
                enrollment.level(),
                enrollment.credits(),
                enrollment.totalHours(),
                enrollment.group(),
                enrollment.subgroup(),
                enrollment.professor(),
                enrollment.campus(),
                enrollment.startDate(),
                enrollment.endDate(),
                enrollment.color(),
                enrollment.meetings().stream().map(ScheduleMapper::toResponse).toList());
    }

    public static MeetingResponse toResponse(Meeting meeting) {
        return new MeetingResponse(
                meeting.meetingId(),
                meeting.dayOfWeek(),
                meeting.startTime(),
                meeting.endTime(),
                meeting.periods().stream().map(ScheduleMapper::toResponse).toList());
    }

    public static MeetingPeriodResponse toResponse(MeetingPeriod period) {
        return new MeetingPeriodResponse(period.from(), period.to(), period.room());
    }

    /**
     * @param resolvedRoom the room in force during the period that matched the requested
     *                      date - not necessarily {@code meeting}'s only room, and
     *                      possibly {@code null}
     */
    public static ClassOccurrenceResponse toClassOccurrence(Enrollment enrollment, Meeting meeting,
                                                            String resolvedRoom) {
        return new ClassOccurrenceResponse(
                enrollment.enrollmentId(),
                enrollment.courseCode(),
                enrollment.courseName(),
                enrollment.professor(),
                meeting.startTime(),
                meeting.endTime(),
                resolvedRoom,
                enrollment.campus(),
                enrollment.color());
    }
}
