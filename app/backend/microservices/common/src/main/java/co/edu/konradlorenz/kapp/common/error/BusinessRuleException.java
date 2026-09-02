package co.edu.konradlorenz.kapp.common.error;

import java.util.List;

/**
 * A domain rule rejected the request as malformed: a grade outside the 0..50 range, a
 * course whose prerequisites are not met. Rendered as HTTP 400 with the offending fields
 * in {@link ApiError#details()}.
 *
 * <p>Do NOT use this for a request that conflicts with existing state - a timetable block
 * overlapping another, a code already taken. That is a 409, and 400 tells the client to
 * fix its payload when the payload is fine. Use {@link ConflictException}, which carries
 * the same {@code details} so the client can be told exactly what it collided with.
 *
 * <p>This is for rules the domain enforces, not for bean-validation failures, which
 * Spring reports on its own.
 */
public class BusinessRuleException extends RuntimeException {

    private final transient List<ApiError.FieldIssue> details;

    public BusinessRuleException(String message) {
        this(message, List.of());
    }

    public BusinessRuleException(String message, List<ApiError.FieldIssue> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<ApiError.FieldIssue> getDetails() {
        return details;
    }
}
