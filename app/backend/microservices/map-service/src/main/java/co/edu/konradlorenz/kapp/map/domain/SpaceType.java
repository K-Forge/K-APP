package co.edu.konradlorenz.kapp.map.domain;

/**
 * The kind of space, as published in the {@code SpaceType} schema of
 * {@code docs/api/map.openapi.yaml}.
 *
 * <p>Clients use it three times now: to choose how a cell is drawn on the schematic floor,
 * to pick its icon, and to offer the {@code type} filter in search. The names are part of
 * the contract, so a value cannot be renamed without a spec change.
 *
 * <p>{@code ELEVATOR}, {@code STAIRS} and {@code ENTRANCE} are what make
 * {@code accessVia} answerable - "piso 4, sube por el ascensor central" needs the lift to
 * be a space with a code, not a decoration drawn on top of one.
 *
 * <p>{@code OFFICE} and {@code ADMIN_OFFICE} are deliberately both here and mean different
 * things to somebody looking for a room: {@code OFFICE} is a person's office, the one a
 * student visits during a lecturer's consulting hours; {@code ADMIN_OFFICE} is a service
 * counter - Registro, Admisiones, Tesorería - where the queue is the point.
 *
 * <p>{@code WELLBEING} covers Bienestar Universitario - counselling, sports and culture -
 * which the university treats as one service area rather than as ordinary offices.
 */
public enum SpaceType {
    CLASSROOM,
    LAB,
    AUDITORIUM,
    LIBRARY,
    CAFETERIA,
    RESTROOM,
    OFFICE,
    ADMIN_OFFICE,
    WELLBEING,
    TERRACE,
    ELEVATOR,
    STAIRS,
    CORRIDOR,
    ENTRANCE,
    OTHER
}
