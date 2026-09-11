package co.edu.konradlorenz.kapp.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * The single error envelope every KApp service returns.
 *
 * <p>Its shape is part of the published API contract: it matches the {@code ApiError}
 * schema in every {@code docs/api/*.openapi.yaml}. Changing a field here is a breaking
 * change for the mobile clients, so change the spec first.
 *
 * @param timestamp when the failure happened
 * @param status    the HTTP status code, repeated in the body so clients that only log
 *                  the payload still see it
 * @param error     the HTTP reason phrase, e.g. {@code Bad Request}
 * @param message   a human-readable description, safe to show to a developer but never
 *                  containing credentials, tokens or stack traces
 * @param path      the request path that failed
 * @param details   per-field problems for validation failures; omitted when empty
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldIssue> details
) {

    /**
     * One field-level problem.
     *
     * @param field the offending field, in dotted notation for nested objects
     * @param issue what is wrong with it
     */
    public record FieldIssue(String field, String issue) {
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }

    public static ApiError of(int status, String error, String message, String path,
                              List<FieldIssue> details) {
        return new ApiError(Instant.now(), status, error, message, path,
                details == null ? List.of() : List.copyOf(details));
    }
}
