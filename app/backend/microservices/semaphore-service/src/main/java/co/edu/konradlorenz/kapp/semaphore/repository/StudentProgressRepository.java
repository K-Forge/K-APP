package co.edu.konradlorenz.kapp.semaphore.repository;

import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StudentProgressRepository extends MongoRepository<StudentProgress, String> {

    Optional<StudentProgress> findByUserId(String userId);

    Optional<StudentProgress> findByUserIdAndPensumCode(String userId, String pensumCode);
}
