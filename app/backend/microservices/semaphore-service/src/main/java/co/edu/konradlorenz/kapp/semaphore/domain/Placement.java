package co.edu.konradlorenz.kapp.semaphore.domain;

/**
 * One course pinned to a level other than the one its pensum gives it.
 *
 * @param code         the course {@code code}, or an elective slot's {@code pensumItemCode} -
 *                     the same identifier every other per-course endpoint accepts, so a client
 *                     never has to hold two ways of naming the same square of the grid
 * @param plannedLevel the level the student intends to take it in
 */
public record Placement(String code, int plannedLevel) {
}
