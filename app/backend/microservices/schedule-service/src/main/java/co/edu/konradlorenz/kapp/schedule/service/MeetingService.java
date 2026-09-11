package co.edu.konradlorenz.kapp.schedule.service;

import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.schedule.domain.Enrollment;
import co.edu.konradlorenz.kapp.schedule.domain.Meeting;
import co.edu.konradlorenz.kapp.schedule.domain.MeetingPeriod;
import co.edu.konradlorenz.kapp.schedule.domain.Schedule;
import co.edu.konradlorenz.kapp.schedule.repository.ScheduleRepository;
import co.edu.konradlorenz.kapp.schedule.web.dto.CreateMeetingRequest;
import co.edu.konradlorenz.kapp.schedule.web.dto.UpdateMeetingRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The {@code /meetings} sub-resource: weekly slots within one enrollment.
 *
 * <p>Every write here goes through the same two gates as {@link EnrollmentService}'s
 * {@code addEnrollment} - {@link MeetingWriteValidation} for the meeting's own internal
 * consistency, then {@link MeetingConflictValidator} against the rest of the schedule -
 * so a meeting can never enter the schedule two different ways with two different rules.
 */
@Service
public class MeetingService {

    private final ScheduleRepository repository;
    private final ScheduleService scheduleService;

    public MeetingService(ScheduleRepository repository, ScheduleService scheduleService) {
        this.repository = repository;
        this.scheduleService = scheduleService;
    }

    public Meeting addMeeting(String userId, String enrollmentId, CreateMeetingRequest request) {
        Schedule schedule = scheduleService.activeOrThrow(userId);
        Enrollment enrollment = enrollmentOrThrow(schedule, enrollmentId);

        Meeting meeting = MeetingFactory.from(request);
        MeetingWriteValidation.validate(meeting);
        MeetingConflictValidator.ensureNoConflicts(schedule, null, enrollmentId,
                enrollment.courseCode(), enrollment.courseName(), List.of(meeting));

        Schedule saved = repository.save(schedule.replacingEnrollment(enrollment.addingMeeting(meeting)));
        return saved.enrollment(enrollmentId).flatMap(e -> e.meeting(meeting.meetingId())).orElseThrow();
    }

    /**
     * {@code periods}, when present in the patch, replaces the whole list rather than
     * merging into it - the contract is explicit that a delta would make the disjoint
     * ranges ambiguous - so the candidate below is built from the complete new list, not
     * appended to the old one.
     */
    public Meeting updateMeeting(String userId, String enrollmentId, String meetingId,
                                 UpdateMeetingRequest patch) {
        Schedule schedule = scheduleService.activeOrThrow(userId);
        Enrollment enrollment = enrollmentOrThrow(schedule, enrollmentId);
        Meeting current = enrollment.meeting(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", meetingId));

        List<MeetingPeriod> periods = patch.periods().present()
                ? patch.periods().value().stream()
                        .map(p -> new MeetingPeriod(p.from(), p.to(), p.room()))
                        .toList()
                : current.periods();

        Meeting candidate = new Meeting(meetingId,
                patch.dayOfWeek().orElse(current.dayOfWeek()),
                patch.startTime().orElse(current.startTime()),
                patch.endTime().orElse(current.endTime()),
                periods);

        MeetingWriteValidation.validate(candidate);
        MeetingConflictValidator.ensureNoConflicts(schedule, meetingId, enrollmentId,
                enrollment.courseCode(), enrollment.courseName(), List.of(candidate));

        Schedule saved = repository.save(schedule.replacingEnrollment(enrollment.replacingMeeting(candidate)));
        return saved.enrollment(enrollmentId).flatMap(e -> e.meeting(meetingId)).orElseThrow();
    }

    public void deleteMeeting(String userId, String enrollmentId, String meetingId) {
        Schedule schedule = scheduleService.activeOrThrow(userId);
        Enrollment enrollment = enrollmentOrThrow(schedule, enrollmentId);
        enrollment.meeting(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", meetingId));

        // The enrollment survives even when this was its last meeting, so the course can
        // be re-scheduled later without retyping courseName, credits, professor and so on.
        repository.save(schedule.replacingEnrollment(enrollment.withoutMeeting(meetingId)));
    }

    private static Enrollment enrollmentOrThrow(Schedule schedule, String enrollmentId) {
        return schedule.enrollment(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));
    }
}
