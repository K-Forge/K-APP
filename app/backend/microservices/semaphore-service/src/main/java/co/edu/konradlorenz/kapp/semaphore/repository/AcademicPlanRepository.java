package co.edu.konradlorenz.kapp.semaphore.repository;

import co.edu.konradlorenz.kapp.semaphore.domain.AcademicPlan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicPlanRepository extends MongoRepository<AcademicPlan, String> {

    /**
     * Primary first, then oldest first. The order is part of the response: the app opens on
     * the first entry, so "which plan is primary" never has to be re-derived by the client.
     */
    List<AcademicPlan> findByUserIdOrderByPrimaryDescCreatedAtAsc(String userId);

    List<AcademicPlan> findByUserIdAndPensumCodeOrderByCreatedAtAsc(String userId, String pensumCode);

    Optional<AcademicPlan> findByUserIdAndPensumCodeAndPrimaryIsTrue(String userId, String pensumCode);

    long countByUserIdAndPensumCode(String userId, String pensumCode);
}
