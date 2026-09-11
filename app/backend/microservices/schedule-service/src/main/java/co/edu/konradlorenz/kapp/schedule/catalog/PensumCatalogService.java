package co.edu.konradlorenz.kapp.schedule.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * The pensum a student's courses are checked against when they build their timetable.
 *
 * <p>Two decisions matter here, both driven by the same fact: the catalogue is near-static
 * reference data owned by another service that is being built in parallel.
 *
 * <ul>
 *   <li><b>Cached for about an hour.</b> A pensum does not change during a semester, so an
 *       hour-old answer is as good as a fresh one, and it means this service does not hit
 *       semaphore-service on every enrollment write.</li>
 *   <li><b>Fails open.</b> If semaphore-service is briefly down and nothing is cached yet,
 *       {@link #pensumCourses} returns empty rather than throwing. Building a
 *       timetable is this service's job; cross-checking it against the catalogue is a
 *       courtesy, not a hard dependency, so a stranger service being down must never block
 *       a student from entering their own schedule by hand.</li>
 * </ul>
 */
@Service
public class PensumCatalogService {

    private static final Logger log = LoggerFactory.getLogger(PensumCatalogService.class);

    private final CatalogClient client;

    public PensumCatalogService(CatalogClient client) {
        this.client = client;
    }

    @Cacheable("pensumCourses")
    public List<PensumCourseView> pensumCourses(String pensumCode) {
        try {
            return client.listPensumCourses(pensumCode);
        } catch (RuntimeException e) {
            log.warn("Could not read pensum {} from semaphore-service; "
                    + "continuing without catalogue validation", pensumCode, e);
            return List.of();
        }
    }

    /**
     * @return the pensum item matching either the course code or, for an elective slot
     * which has no code, the pensum item code - empty when the catalogue could not be
     * read, or the item is genuinely not part of this pensum
     */
    public Optional<PensumCourseView> find(String pensumCode, String courseCode, String pensumItemCode) {
        return pensumCourses(pensumCode).stream()
                .filter(item -> item.pensumItemCode().equals(pensumItemCode)
                        || (item.code() != null && item.code().equals(courseCode)))
                .findFirst();
    }
}
