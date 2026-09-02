package co.edu.konradlorenz.kapp.schedule.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.schedule.catalog.CurriculumCatalogService;
import co.edu.konradlorenz.kapp.schedule.domain.Enrollment;
import co.edu.konradlorenz.kapp.schedule.domain.Meeting;
import co.edu.konradlorenz.kapp.schedule.domain.Schedule;
import co.edu.konradlorenz.kapp.schedule.repository.ScheduleRepository;
import co.edu.konradlorenz.kapp.schedule.web.dto.CreateEnrollmentRequest;
import co.edu.konradlorenz.kapp.schedule.web.dto.CreateMeetingRequest;
import co.edu.konradlorenz.kapp.schedule.web.dto.UpdateEnrollmentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adding, editing and removing the courses inside a schedule.
 *
 * <p>Meetings are not edited here beyond what {@link CreateEnrollmentRequest} carries at
 * creation time - see {@link MeetingService} for the {@code /meetings} sub-resource.
 */
@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final ScheduleRepository repository;
    private final ScheduleService scheduleService;
    private final CurriculumCatalogService catalog;

    public EnrollmentService(ScheduleRepository repository, ScheduleService scheduleService,
                             CurriculumCatalogService catalog) {
        this.repository = repository;
        this.scheduleService = scheduleService;
        this.catalog = catalog;
    }

    /**
     * Adds a course, with its weekly meetings, to the caller's active schedule.
     *
     * <p>Every meeting is checked, in order, against every meeting already in the
     * schedule and against every meeting earlier in this same request - see
     * {@link MeetingConflictValidator}. On any conflict nothing is written.
     */
    public Enrollment addEnrollment(String userId, CreateEnrollmentRequest request) {
        Schedule schedule = repository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active schedule found; create one first"));

        String enrollmentId = UUID.randomUUID().toString();
        List<Meeting> meetings = new ArrayList<>();
        for (CreateMeetingRequest meetingRequest : request.meetings()) {
            Meeting meeting = MeetingFactory.from(meetingRequest);
            MeetingWriteValidation.validate(meeting);
            meetings.add(meeting);
        }

        MeetingConflictValidator.ensureNoConflicts(schedule, null, enrollmentId,
                request.courseCode(), request.courseName(), meetings);

        if (catalog.find(schedule.pensumCode(), request.courseCode(), request.pensumItemCode()).isEmpty()) {
            // Not a rejection: the catalogue is a courtesy cross-check, not the source of
            // truth for a hand-entered timetable, and may simply be unreachable right now.
            log.debug("Course {} ({}) not found in curriculum {} for user {}; "
                            + "storing the write-time snapshot as sent",
                    request.courseCode(), request.pensumItemCode(), schedule.pensumCode(), userId);
        }

        Enrollment enrollment = new Enrollment(enrollmentId, request.courseCode(), request.pensumItemCode(),
                request.courseName(), request.level(), request.credits(), request.totalHours(),
                request.group(), request.subgroup(), request.professor(), request.campus(),
                request.startDate(), request.endDate(), request.color(), meetings);

        Schedule saved = repository.save(schedule.addingEnrollment(enrollment));
        return saved.enrollment(enrollmentId).orElseThrow();
    }

    public Enrollment updateEnrollment(String userId, String enrollmentId, UpdateEnrollmentRequest patch) {
        Schedule schedule = scheduleService.activeOrThrow(userId);
        Enrollment current = schedule.enrollment(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));

        Enrollment updated = new Enrollment(
                current.enrollmentId(),
                patch.courseCode().orElse(current.courseCode()),
                patch.pensumItemCode().orElse(current.pensumItemCode()),
                patch.courseName().orElse(current.courseName()),
                patch.level().orElse(current.level()),
                patch.credits().orElse(current.credits()),
                patch.totalHours().orElse(current.totalHours()),
                patch.group().orElse(current.group()),
                patch.subgroup().orElse(current.subgroup()),
                patch.professor().orElse(current.professor()),
                patch.campus().orElse(current.campus()),
                patch.startDate().orElse(current.startDate()),
                patch.endDate().orElse(current.endDate()),
                patch.color().orElse(current.color()),
                // Meetings are untouched by this operation, so it can never conflict.
                current.meetings());

        if (updated.endDate().isBefore(updated.startDate())) {
            throw new BusinessRuleException("Validation failed",
                    List.of(new ApiError.FieldIssue("endDate", "must not be earlier than startDate")));
        }

        Schedule saved = repository.save(schedule.replacingEnrollment(updated));
        return saved.enrollment(enrollmentId).orElseThrow();
    }

    public void deleteEnrollment(String userId, String enrollmentId) {
        Schedule schedule = scheduleService.activeOrThrow(userId);
        schedule.enrollment(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));

        repository.save(schedule.withoutEnrollment(enrollmentId));
    }
}
