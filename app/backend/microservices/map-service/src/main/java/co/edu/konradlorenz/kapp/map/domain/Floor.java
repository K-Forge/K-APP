package co.edu.konradlorenz.kapp.map.domain;

/**
 * One floor of a building, embedded in its {@link BuildingDocument}.
 *
 * <p>Floors are embedded rather than kept in their own collection because they are never
 * queried on their own: every read that wants a floor already knows the building. Spaces
 * are the opposite case and live in a flat collection of their own.
 *
 * @param level         floor number; matches the first digit of the room codes on it, so
 *                      room {@code 708} is on level 7
 * @param name          display name in Spanish, as shown to students ("Piso 7")
 * @param planImageUrl  path to the static plan image. A path, never an absolute URL: the
 *                      reverse proxy serves these files, this service never does
 * @param imageWidth    intrinsic width of the plan image in pixels
 * @param imageHeight   intrinsic height of the plan image in pixels
 */
public record Floor(
        int level,
        String name,
        String planImageUrl,
        int imageWidth,
        int imageHeight
) {
}
