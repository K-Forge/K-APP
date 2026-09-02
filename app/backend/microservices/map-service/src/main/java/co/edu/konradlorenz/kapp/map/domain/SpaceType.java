package co.edu.konradlorenz.kapp.map.domain;

/**
 * The kind of space, as published in the {@code SpaceType} schema of
 * {@code docs/api/map.openapi.yaml}.
 *
 * <p>Clients use it twice: to pick the pin icon drawn on the floor plan, and to offer the
 * {@code type} filter in search. The names are part of the contract, so a value cannot be
 * renamed without a spec change.
 *
 * <p>{@code WELLBEING} covers Bienestar Universitario - counselling, sports and culture -
 * which the university treats as one service area rather than as ordinary offices.
 */
public enum SpaceType {
    CLASSROOM,
    LAB,
    AUDITORIUM,
    OFFICE,
    LIBRARY,
    CAFETERIA,
    RESTROOM,
    WELLBEING,
    OTHER
}
