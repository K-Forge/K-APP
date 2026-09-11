package co.edu.konradlorenz.kapp.common.error;

import java.util.List;

/**
 * The request is well-formed but collides with state that already exists: a timetable
 * meeting overlapping another, a building that still holds spaces, a pensum edited
 * concurrently. Rendered as HTTP 409 with the collision described in
 * {@link ApiError#details()}.
 *
 * <p>It exists because neither neighbour fits this case. {@link BusinessRuleException} is
 * a 400, which tells the client to fix a payload that is not actually wrong.
 * {@link DuplicateResourceException} is a 409 but carries no details, so the client learns
 * that something collided without learning what — and "your timetable has a conflict" is
 * useless without naming the class it conflicts with.
 *
 * <p>{@code schedule-service} needed exactly this and had to declare its own exception and
 * advice locally. Anything with the same need should use this instead of repeating that.
 */
public class ConflictException extends RuntimeException {

    private final transient List<ApiError.FieldIssue> details;

    public ConflictException(String message) {
        this(message, List.of());
    }

    /**
     * @param details what the request collided with, specific enough to act on — the
     *                identifier of the conflicting entity, not merely the field name
     */
    public ConflictException(String message, List<ApiError.FieldIssue> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<ApiError.FieldIssue> getDetails() {
        return details;
    }
}
