package co.edu.konradlorenz.kapp.map.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A locatable space: a classroom, a lab, an office, a bathroom - anything a student may
 * need to find.
 *
 * <p>Spaces are a flat collection rather than an array nested inside the building. The
 * primary access pattern is "find a room by name anywhere on campus", and a nested model
 * would hand the client whole building documents to filter for itself. Flat also means the
 * one text index this collection is allowed can cover every searchable field at once.
 *
 * <p>{@code buildingCode}, {@code campus} and {@code floorLevel} are denormalised copies of
 * the building's own fields. A search result can then be rendered without a second call,
 * which is the whole point of the flat model. The cost is that
 * {@code BuildingService.update} has to propagate a changed code or campus down to every
 * space in the building, and it does.
 *
 * <p>{@code x} and {@code y} are PERCENTAGES of the plan image (0-100), never pixels, so a
 * pin lands on the same spot whatever size the image is rendered at.
 *
 * <p>{@code code} is the room code printed on the door and is unique within a building, not
 * across the map: two blocks can each have a room 302. It is the same code
 * {@code schedule-service} stores against a class, which is what lets a student tap a class
 * and land on its classroom.
 */
@Document(collection = "spaces")
public record SpaceDocument(
        @Id String id,
        String code,
        String name,
        SpaceType type,
        String buildingId,
        String buildingCode,
        String campus,
        int floorLevel,
        List<String> aliases,
        double x,
        double y,
        Integer capacity,
        boolean placeholder,
        Instant createdAt,
        Instant updatedAt
) {

    public SpaceDocument {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }
}
