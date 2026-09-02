package co.edu.konradlorenz.kapp.map.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spaces by code, by building and by floor. The full-text search is not here: it needs the
 * {@code $meta} text score for ranking, which derived query methods cannot express, so it
 * lives in {@link co.edu.konradlorenz.kapp.map.service.SpaceSearch}.
 */
public interface SpaceRepository extends MongoRepository<SpaceDocument, String> {

    /**
     * Every space carrying this room code, across all buildings. More than one result is a
     * normal situation - codes are unique per building - and is what makes the
     * {@code buildingCode} disambiguator necessary.
     */
    List<SpaceDocument> findByCodeOrderByBuildingCodeAsc(String code);

    Optional<SpaceDocument> findByBuildingIdAndCode(String buildingId, String code);

    List<SpaceDocument> findByBuildingIdAndFloorLevelOrderByCodeAsc(String buildingId, int floorLevel);

    boolean existsByBuildingId(String buildingId);

    boolean existsByBuildingIdAndFloorLevel(String buildingId, int floorLevel);

    List<SpaceDocument> findByBuildingId(String buildingId);

    long countByPlaceholderIsTrue();
}
