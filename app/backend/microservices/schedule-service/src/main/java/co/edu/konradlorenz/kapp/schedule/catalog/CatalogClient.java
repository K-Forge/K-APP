package co.edu.konradlorenz.kapp.schedule.catalog;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Reads the academic catalogue from {@code semaphore-service}, which owns it.
 *
 * <p>{@code name} matches {@code spring.application.name} in semaphore-service's own
 * {@code application.yml}, so Eureka resolves the call; no URL is hardcoded. The caller's
 * own bearer token is attached automatically by {@code common}'s
 * {@code KappFeignAutoConfiguration} - nothing here has to touch a token.
 *
 * <p>semaphore-service is being built in parallel and may not be reachable while this
 * service runs; see {@link PensumCatalogService} for how that is handled, and mock
 * this interface directly in tests rather than standing up a real semaphore-service.
 */
@FeignClient(name = "semaphore-service", path = "/api/catalog")
public interface CatalogClient {

    @GetMapping("/pensums/{pensumCode}/courses")
    List<PensumCourseView> listPensumCourses(@PathVariable("pensumCode") String pensumCode);
}
