package co.edu.konradlorenz.kapp.auth.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VisitorPassRepository extends MongoRepository<VisitorPass, String> {

    Optional<VisitorPass> findByCode(String code);

    boolean existsByCode(String code);

    /** Newest first: reception is almost always looking at today. */
    List<VisitorPass> findAllByOrderByCreatedAtDesc();

    List<VisitorPass> findByRedeemedAtIsNotNullOrderByRedeemedAtDesc();

    List<VisitorPass> findByRedeemedAtIsNullOrderByCreatedAtDesc();
}
