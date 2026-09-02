package co.edu.konradlorenz.kapp.schedule.service;

import co.edu.konradlorenz.kapp.schedule.domain.Meeting;
import co.edu.konradlorenz.kapp.schedule.domain.MeetingPeriod;
import co.edu.konradlorenz.kapp.schedule.web.dto.CreateMeetingRequest;

import java.util.List;
import java.util.UUID;

/**
 * Assembles a new {@link Meeting}, with a fresh server-assigned id, from a validated
 * request. Shared by {@link EnrollmentService} (one call per entry of {@code meetings[]})
 * and {@link MeetingService} (one call for the single meeting being added), so a new
 * meeting is only ever built in one place.
 */
final class MeetingFactory {

    private MeetingFactory() {
    }

    static Meeting from(CreateMeetingRequest request) {
        List<MeetingPeriod> periods = request.periods().stream()
                .map(p -> new MeetingPeriod(p.from(), p.to(), p.room()))
                .toList();
        return new Meeting(UUID.randomUUID().toString(), request.dayOfWeek(),
                request.startTime(), request.endTime(), periods);
    }
}
