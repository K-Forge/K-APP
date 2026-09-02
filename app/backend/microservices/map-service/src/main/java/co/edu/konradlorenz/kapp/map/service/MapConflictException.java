package co.edu.konradlorenz.kapp.map.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;

import java.util.List;

/**
 * The request conflicts with the current state of the map and nothing was changed.
 * Rendered as HTTP 409 by {@code MapExceptionHandler}.
 *
 * <p>Two cases raise it: deleting a building that still has spaces, and dropping a floor
 * from a building while spaces still sit on it. Neither is a duplicate key, so
 * {@code common}'s {@code DuplicateResourceException} would be a lie in the logs; and
 * unlike that exception this one can carry {@code details}, which the contract needs for
 * the ambiguous-code case.
 *
 * <p>Deliberately a refusal rather than a cascade. A cascading delete of a building would
 * silently take several hundred hand-placed pins with it, and the person who typed the
 * wrong code would find out weeks later.
 */
public class MapConflictException extends RuntimeException {

    private final transient List<ApiError.FieldIssue> details;

    public MapConflictException(String message) {
        this(message, List.of());
    }

    public MapConflictException(String message, List<ApiError.FieldIssue> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<ApiError.FieldIssue> getDetails() {
        return details;
    }
}
