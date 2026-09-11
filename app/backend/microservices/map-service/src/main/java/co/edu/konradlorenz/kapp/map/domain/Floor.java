package co.edu.konradlorenz.kapp.map.domain;

import java.util.List;

/**
 * One floor of a building, embedded in its {@link BuildingDocument}.
 *
 * <p>Floors are embedded rather than kept in their own collection because they are never
 * queried on their own: every read that wants a floor already knows the building. Spaces
 * are the opposite case and live in a flat collection of their own.
 *
 * <h2>A grid, not a photograph</h2>
 * This used to carry {@code planImageUrl} and the image's pixel dimensions, with each space
 * pinned at a percentage of it. The floor is now described as data - rooms occupying cells
 * of a grid, corridors tracing paths through it - and the client draws it.
 *
 * <p>That is not only a nicer rendering. Obtaining architectural plans for five buildings
 * depended on other people's calendars and was the single likeliest thing to slip before
 * November. A schematic floor is captured by walking it with the grid editor, which is work
 * the team can do itself, in an afternoon, without asking anybody's permission.
 *
 * @param level       floor number. Matches the first digit of the room codes on it, so room
 *                    {@code 708} is on level 7 - except in the basement, which is level
 *                    {@code -1} and whose rooms are coded however the building codes them
 * @param gridRows    how many rows the floor's grid has
 * @param gridColumns how many columns it has
 * @param corridors   walkable routes across this floor
 */
public record Floor(
        int level,
        String name,
        int gridRows,
        int gridColumns,
        List<Corridor> corridors
) {

    public Floor {
        corridors = corridors == null ? List.of() : List.copyOf(corridors);
    }
}
