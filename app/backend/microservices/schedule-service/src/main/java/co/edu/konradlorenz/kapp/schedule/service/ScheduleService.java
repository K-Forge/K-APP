package co.edu.konradlorenz.kapp.schedule.service;

import co.edu.konradlorenz.kapp.common.error.DuplicateResourceException;
import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.schedule.domain.Enrollment;
import co.edu.konradlorenz.kapp.schedule.domain.Meeting;
import co.edu.konradlorenz.kapp.schedule.domain.MeetingResolution;
import co.edu.konradlorenz.kapp.schedule.domain.Schedule;
import co.edu.konradlorenz.kapp.schedule.mapper.ScheduleMapper;
import co.edu.konradlorenz.kapp.schedule.repository.ScheduleRepository;
import co.edu.konradlorenz.kapp.schedule.web.dto.ClassOccurrenceResponse;
import co.edu.konradlorenz.kapp.schedule.web.dto.CreateScheduleRequest;
import co.edu.konradlorenz.kapp.schedule.web.dto.SchedulePeriodSummaryResponse;
import co.edu.konradlorenz.kapp.schedule.web.dto.WeekAgendaResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The schedule container itself: creation, retrieval, deletion, the period switcher, and
 * the day/week resolution that is the whole reason the nested model exists.
 *
 * <p>Enrollment and meeting mutations live in {@link EnrollmentService} and
 * {@link MeetingService}; this class owns the schedule document as a whole.
 */
@Service
public class ScheduleService {

    private final ScheduleRepository repository;
    private final MongoTemplate mongoTemplate;

    public ScheduleService(ScheduleRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /** @param period null resolves to the caller's currently active schedule */
    public Schedule getSchedule(String userId, String period) {
        return period == null
                ? activeOrThrow(userId)
                : repository.findByUserIdAndPeriod(userId, period)
                        .orElseThrow(() -> new ResourceNotFoundException("No schedule found for period " + period));
    }

    /** Same lookup as {@link #getSchedule}, with the messages {@code GET /{userId}} uses. */
    public Schedule getScheduleForAdmin(String userId, String period) {
        if (period == null) {
            return repository.findByUserIdAndActiveTrue(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No active schedule found for user " + userId));
        }
        return repository.findByUserIdAndPeriod(userId, period)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No schedule found for user " + userId + " in period " + period));
    }

    public List<SchedulePeriodSummaryResponse> listPeriods(String userId) {
        return repository.findByUserIdOrderByPeriodDesc(userId).stream()
                .map(s -> new SchedulePeriodSummaryResponse(s.period(), s.active(), s.enrollments().size()))
                .toList();
    }

    /**
     * Creates an empty schedule for one period, and demotes whatever schedule was active
     * before it: exactly one schedule is active per student at a time.
     *
     * <p>Existence is checked before anything is demoted, so a duplicate request fails
     * cleanly with 409 and leaves the previously active schedule untouched - deactivating
     * first and only then discovering the insert cannot proceed would silently strand the
     * student with no active schedule at all.
     */
    public Schedule createSchedule(String userId, CreateScheduleRequest request) {
        if (repository.findByUserIdAndPeriod(userId, request.period()).isPresent()) {
            throw new DuplicateResourceException(
                    "A schedule for period " + request.period() + " already exists");
        }

        mongoTemplate.updateMulti(
                Query.query(Criteria.where("userId").is(userId).and("active").is(true)),
                Update.update("active", false),
                Schedule.class);

        Schedule toCreate = new Schedule(UUID.randomUUID().toString(), userId, request.period(),
                request.programCode(), request.pensumCode(), request.level(), true, List.of());
        try {
            return repository.save(toCreate);
        } catch (DuplicateKeyException race) {
            // Two concurrent creates for the same period: the unique index is the real
            // guard, the pre-check above only avoids the common case touching `active`.
            throw new DuplicateResourceException(
                    "A schedule for period " + request.period() + " already exists");
        }
    }

    public void deleteSchedule(String userId, String period) {
        Schedule schedule = repository.findByUserIdAndPeriod(userId, period)
                .orElseThrow(() -> new ResourceNotFoundException("No schedule found for period " + period));
        repository.delete(schedule);
    }

    public List<ClassOccurrenceResponse> day(String userId, LocalDate date) {
        return occurrencesOn(activeOrThrow(userId), date);
    }

    public WeekAgendaResponse week(String userId, LocalDate anyDateInWeek) {
        Schedule schedule = activeOrThrow(userId);
        LocalDate monday = (anyDateInWeek == null ? LocalDate.now() : anyDateInWeek).with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        Map<DayOfWeek, List<ClassOccurrenceResponse>> days = new EnumMap<>(DayOfWeek.class);
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = monday.plusDays(offset);
            days.put(date.getDayOfWeek(), occurrencesOn(schedule, date));
        }
        return new WeekAgendaResponse(monday, sunday, days);
    }

    Schedule activeOrThrow(String userId) {
        return repository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active schedule found"));
    }

    /**
     * The single place a date is resolved against every meeting in a schedule - used
     * identically by {@link #day} and, once per date of the week, by {@link #week}, which
     * is what keeps the two endpoints from re-implementing the resolution rule twice.
     */
    private List<ClassOccurrenceResponse> occurrencesOn(Schedule schedule, LocalDate date) {
        List<ClassOccurrenceResponse> result = new ArrayList<>();
        for (Enrollment enrollment : schedule.enrollments()) {
            for (Meeting meeting : enrollment.meetings()) {
                MeetingResolution.periodOn(meeting, date).ifPresent(period ->
                        result.add(ScheduleMapper.toClassOccurrence(enrollment, meeting, period.room())));
            }
        }
        result.sort(Comparator.comparing(ClassOccurrenceResponse::startTime));
        return result;
    }
}
