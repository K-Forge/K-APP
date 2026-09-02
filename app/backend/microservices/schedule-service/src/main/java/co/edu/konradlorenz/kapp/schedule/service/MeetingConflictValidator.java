package co.edu.konradlorenz.kapp.schedule.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.schedule.domain.Enrollment;
import co.edu.konradlorenz.kapp.schedule.domain.Meeting;
import co.edu.konradlorenz.kapp.schedule.domain.MeetingOverlap;
import co.edu.konradlorenz.kapp.schedule.domain.Schedule;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns {@link MeetingOverlap#conflicts} into the write-time gate the contract requires:
 * check one or more incoming meetings against a schedule and reject the whole write with a
 * 409 naming every collision, or write nothing.
 *
 * <p>One method serves every write path that can produce a conflict -
 * {@code POST /enrollments} (many new meetings at once, which must also be checked against
 * each other), {@code POST .../meetings} (one new meeting) and
 * {@code PATCH .../meetings/{meetingId}} (one replacement meeting, excluded from the
 * comparison against itself) - so the "checked against the rest of the schedule, and
 * against the others in this request" rule in the contract is implemented exactly once.
 */
final class MeetingConflictValidator {

    private MeetingConflictValidator() {
    }

    /**
     * @param schedule           the schedule the incoming meetings would join
     * @param excludingMeetingId a meeting id to leave out of the "already in the schedule"
     *                           side of the comparison - the meeting being replaced by an
     *                           update, so it is not compared against itself. Null when
     *                           nothing should be excluded (adding, rather than updating).
     * @param owningEnrollmentId identity of the enrollment {@code incoming} belongs to,
     *                           for the message when new meetings conflict with each other
     * @param owningCourseCode   snapshot used in the same message
     * @param owningCourseName   snapshot used in the same message
     * @param incoming           the meeting(s) being introduced by this request, in the
     *                           order they appear in the request body
     */
    static void ensureNoConflicts(Schedule schedule, String excludingMeetingId,
                                  String owningEnrollmentId, String owningCourseCode,
                                  String owningCourseName, List<Meeting> incoming) {
        List<KnownMeeting> against = new ArrayList<>();
        for (Enrollment enrollment : schedule.enrollments()) {
            for (Meeting meeting : enrollment.meetings()) {
                if (!meeting.meetingId().equals(excludingMeetingId)) {
                    against.add(new KnownMeeting(meeting, enrollment.enrollmentId(),
                            enrollment.courseCode(), enrollment.courseName()));
                }
            }
        }

        List<ApiError.FieldIssue> issues = new ArrayList<>();
        for (int i = 0; i < incoming.size(); i++) {
            Meeting candidate = incoming.get(i);
            String field = "meetings[" + i + "]";
            for (KnownMeeting other : against) {
                MeetingOverlap.findConflict(candidate, other.meeting()).ifPresent(overlap ->
                        issues.add(new ApiError.FieldIssue(field, describe(other, candidate, overlap))));
            }
            against.add(new KnownMeeting(candidate, owningEnrollmentId, owningCourseCode, owningCourseName));
        }

        if (!issues.isEmpty()) {
            throw new MeetingConflictException(
                    "The meeting conflicts with a class already in the schedule", issues);
        }
    }

    private static String describe(KnownMeeting existing, Meeting requested,
                                    MeetingOverlap.PeriodOverlap overlap) {
        return "Conflicts with enrollment %s (course %s %s), meeting %s on %s %s-%s; "
                .formatted(existing.enrollmentId(), existing.courseCode(), existing.courseName(),
                        existing.meeting().meetingId(), existing.meeting().dayOfWeek(),
                        existing.meeting().startTime(), existing.meeting().endTime())
                + "the requested %s-%s overlaps it, and the date ranges %s..%s and %s..%s intersect"
                        .formatted(requested.startTime(), requested.endTime(),
                                overlap.second().from(), overlap.second().to(),
                                overlap.first().from(), overlap.first().to());
    }

    /** A meeting plus the identity of the enrollment it belongs (or would belong) to. */
    private record KnownMeeting(Meeting meeting, String enrollmentId, String courseCode, String courseName) {
    }
}
