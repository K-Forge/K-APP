package co.edu.konradlorenz.kapp.schedule.repository;

import co.edu.konradlorenz.kapp.schedule.domain.Schedule;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * One document per student per period. Every finder here is a prefix of, or the whole of,
 * one of the two indexes {@code V001_ScheduleIndexes} creates
 * ({@code (userId, period)} unique, {@code (userId, active)}), so each resolves to a
 * narrow index scan rather than a collection scan - see section 3 of
 * {@code docs/INTEGRATION-NOTES.md} on proving a query plan is actually narrow, not merely
 * an {@code IXSCAN} in name.
 */
public interface ScheduleRepository extends MongoRepository<Schedule, String> {

    /** Backed by the unique {@code (userId, period)} index: at most one match. */
    Optional<Schedule> findByUserIdAndPeriod(String userId, String period);

    /** Backed by the {@code (userId, active)} index: "my current timetable". */
    Optional<Schedule> findByUserIdAndActiveTrue(String userId);

    /**
     * Newest period first, for the period switcher. The unique {@code (userId, period)}
     * index is ascending on both keys, so MongoDB can walk it in reverse to satisfy this
     * sort without an in-memory sort stage.
     */
    List<Schedule> findByUserIdOrderByPeriodDesc(String userId);
}
