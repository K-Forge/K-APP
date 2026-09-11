package co.edu.konradlorenz.kapp.map.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A locatable space: a classroom, a lab, an office, a bathroom, a lift - anything a student
 * may need to find.
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
 * <h2>Grid cells, not pins on a photograph</h2>
 * A space occupies a rectangle of the floor's grid: {@code gridRow} and {@code gridColumn}
 * are its top-left cell, {@code rowSpan} and {@code colSpan} its size. Both spans default to
 * 1, so an ordinary classroom is one cell and only an auditorium has to say otherwise.
 *
 * @param code       the code printed on the door, including its wing where the building uses
 *                   them - {@code "301-N"}. Unique within a building, not across campus: two
 *                   blocks can each have a room 302. This is the same code
 *                   {@code schedule-service} stores against a class, which is what lets a
 *                   student tap a class and land on its classroom
 * @param baseCode   the code without the wing - {@code "301"} for {@code "301-N"}. Stored so
 *                   a student who types what they were told, without the wing, still finds
 *                   all three rooms
 * @param wing       which arm of the floor it is in, or null in a building with one arm
 * @param accessVia  code of the lift, staircase or entrance that serves this space. This is
 *                   what produces "piso 4, sube por el ascensor central". Explicit rather
 *                   than derived from proximity: whoever walks the floor knows which lift
 *                   people actually use, and nearest-by-grid-distance would confidently give
 *                   the wrong one whenever a wall sits between them
 */
@Document(collection = "spaces")
public record SpaceDocument(
        @Id String id,
        String code,
        String baseCode,
        Wing wing,
        String name,
        SpaceType type,
        String buildingId,
        String buildingCode,
        String campus,
        int floorLevel,
        List<String> aliases,
        int gridRow,
        int gridColumn,
        int rowSpan,
        int colSpan,
        String accessVia,
        Integer capacity,
        boolean placeholder,
        Instant createdAt,
        Instant updatedAt
) {

    public SpaceDocument {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        rowSpan = rowSpan < 1 ? 1 : rowSpan;
        colSpan = colSpan < 1 ? 1 : colSpan;
    }

    /**
     * The code with its wing suffix removed: {@code "301"} for {@code "301-N"}.
     *
     * <p>Only a suffix that actually names a wing is stripped. Splitting on the last dash
     * unconditionally looked simpler and was wrong for most of the codes on campus:
     * {@code "S-01"} in the basement would have had a base code of {@code "S"}, and the lift
     * {@code "ASC-CENTRAL"} one of {@code "ASC"} - so a student searching {@code "S"} would
     * be offered every basement room, and two unrelated staircases would have collapsed into
     * one base code.
     *
     * <p>Applied when a space is written, so both fields are stored rather than re-derived on
     * every read - and so a building that names its wings some other way can supply them
     * itself instead of fighting this.
     */
    public static String baseCodeOf(String code) {
        if (code == null) {
            return null;
        }
        if (wingOf(code) == null) {
            return code;
        }
        return code.substring(0, code.lastIndexOf('-'));
    }

    /**
     * @return the wing implied by a code's suffix, or null when it carries none. Only the
     *         single letters N, S and C name a wing; {@code "ESC-SUR"} is a staircase, not a
     *         room in the south wing
     */
    public static Wing wingOf(String code) {
        if (code == null) {
            return null;
        }
        int dash = code.lastIndexOf('-');
        if (dash <= 0 || dash != code.length() - 2) {
            return null;
        }
        return switch (code.substring(dash + 1).toUpperCase()) {
            case "N" -> Wing.NORTE;
            case "S" -> Wing.SUR;
            case "C" -> Wing.CENTRAL;
            default -> null;
        };
    }

    /** The last cell the space covers, inclusive. */
    public int lastRow() {
        return gridRow + rowSpan - 1;
    }

    public int lastColumn() {
        return gridColumn + colSpan - 1;
    }

    public boolean overlaps(SpaceDocument other) {
        return gridRow <= other.lastRow() && other.gridRow <= lastRow()
                && gridColumn <= other.lastColumn() && other.gridColumn <= lastColumn();
    }
}
