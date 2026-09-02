package co.edu.konradlorenz.kapp.schedule.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * One course the student takes during a period, with its weekly meetings.
 *
 * <p>{@code courseName}, {@code credits}, {@code professor}, {@code totalHours},
 * {@code level}, {@code group}, {@code subgroup} and {@code campus} are snapshots taken
 * when the enrollment was written, never live references to a course catalogue. A
 * timetable entry is a historical record: renaming a course, changing its credits or
 * reassigning its professor later must not alter entries already stored. This is why
 * nothing here points back at {@code semaphore-service}'s catalogue by id.
 *
 * @param subgroup nullable: {@code null} when the group is not subdivided
 */
public record Enrollment(
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
        List<Meeting> meetings
) {

    public Enrollment {
        meetings = meetings == null ? List.of() : List.copyOf(meetings);
    }

    public Enrollment withMeetings(List<Meeting> newMeetings) {
        return new Enrollment(enrollmentId, courseCode, pensumItemCode, courseName, level,
                credits, totalHours, group, subgroup, professor, campus, startDate, endDate,
                color, newMeetings);
    }

    public Optional<Meeting> meeting(String meetingId) {
        return meetings.stream().filter(m -> m.meetingId().equals(meetingId)).findFirst();
    }

    /** @return a copy with {@code meeting} appended */
    public Enrollment addingMeeting(Meeting meeting) {
        List<Meeting> updated = new ArrayList<>(meetings);
        updated.add(meeting);
        return withMeetings(updated);
    }

    /** @return a copy with the meeting sharing {@code updated}'s id replaced */
    public Enrollment replacingMeeting(Meeting updated) {
        return withMeetings(meetings.stream()
                .map(m -> m.meetingId().equals(updated.meetingId()) ? updated : m)
                .collect(Collectors.toList()));
    }

    /** @return a copy with the meeting matching {@code meetingId} removed */
    public Enrollment withoutMeeting(String meetingId) {
        return withMeetings(meetings.stream()
                .filter(m -> !m.meetingId().equals(meetingId))
                .collect(Collectors.toList()));
    }
}
