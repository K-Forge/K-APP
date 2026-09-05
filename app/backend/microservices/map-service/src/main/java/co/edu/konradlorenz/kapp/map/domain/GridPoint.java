package co.edu.konradlorenz.kapp.map.domain;

/**
 * One cell of a floor's grid, used to trace a corridor.
 *
 * <p>Zero-based, row first, matching how the floor is drawn: row 0 is the top edge as the
 * plan is displayed, column 0 the left.
 */
public record GridPoint(int row, int col) {
}
