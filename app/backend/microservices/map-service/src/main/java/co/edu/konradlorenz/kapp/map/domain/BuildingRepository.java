package co.edu.konradlorenz.kapp.map.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Buildings are addressed by {@code code} everywhere in the API, so every finder here is
 * keyed on it. {@code id} exists only because MongoDB needs one and because spaces point
 * at it.
 */
public interface BuildingRepository extends MongoRepository<BuildingDocument, String> {

    Optional<BuildingDocument> findByCode(String code);

    boolean existsByCode(String code);

    List<BuildingDocument> findAllByOrderByCodeAsc();

    List<BuildingDocument> findAllByCampusOrderByCodeAsc(String campus);

    long countByPlaceholderIsTrue();
}
