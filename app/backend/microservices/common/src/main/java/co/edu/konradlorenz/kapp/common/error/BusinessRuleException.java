package co.edu.konradlorenz.kapp.common.error;

import java.util.List;

/**
 * A domain rule rejected the request: a schedule block overlapping another, a grade
 * outside the 0..50 range, a course whose prerequisites are not met. Rendered as
 * HTTP 400 with the offending fields in {@link ApiError#details()}.
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
