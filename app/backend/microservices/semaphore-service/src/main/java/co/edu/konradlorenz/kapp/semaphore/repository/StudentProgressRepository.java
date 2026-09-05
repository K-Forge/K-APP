package co.edu.konradlorenz.kapp.semaphore.repository;

import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StudentProgressRepository extends MongoRepository<StudentProgress, String> {

    Optional<StudentProgress> findByUserId(String userId);

    Optional<StudentProgress> findByUserIdAndPensumCode(String userId, String pensumCode);

    /**
     * How many students are pinned to a pensum. Used to refuse deleting a curriculum
     * that is still in use, rather than leaving their progress pointing at nothing.
     */
    long countByPensumCode(String pensumCode);
}
