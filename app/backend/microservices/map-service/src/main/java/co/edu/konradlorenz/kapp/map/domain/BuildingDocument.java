package co.edu.konradlorenz.kapp.map.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A campus building and the floors it contains.
 *
 * <p>{@code code} is the stable business key: every path parameter in the API addresses a
 * building by code, never by {@code id}. A unique index on it is created by
 * {@code V001_MapIndexes}.
 *
 * <p>{@code placeholder} is storage-only metadata and is deliberately NOT part of the
 * published {@code Building} schema, so it never reaches a client. It marks the invented
 * campus seeded by {@code V002_PlaceholderCampusSeed} while the real floor plans are being
 * obtained, so nobody mistakes seeded data for a survey of the actual campus.
 *
 * @param floors ordered by ascending level by the constructor, so readers never have to
 *               sort and the API's "ordered by ascending level" promise cannot drift
 */
@Document(collection = "buildings")
public record BuildingDocument(
        @Id String id,
        String code,
        String name,
        String campus,
        String description,
        List<Floor> floors,
        boolean placeholder,
        Instant createdAt,
        Instant updatedAt
) {

    public BuildingDocument {
        floors = floors == null
                ? List.of()
                : floors.stream().sorted(Comparator.comparingInt(Floor::level)).toList();
    }

    public Optional<Floor> floorAt(int level) {
        return floors.stream().filter(f -> f.level() == level).findFirst();
    }

    public boolean hasFloor(int level) {
        return floorAt(level).isPresent();
    }
}
