package co.edu.konradlorenz.kapp.schedule.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;

import java.util.List;

/**
 * A meeting in the request conflicts with one already in the schedule, or with another
 * meeting in the same request. Rendered as HTTP 409 by {@code web.ScheduleExceptionHandler}.
 *
 * <p>This is deliberately its own type rather than {@code common}'s
 * {@code BusinessRuleException} (400, despite its javadoc using exactly this scenario as
 * an example) or {@code DuplicateResourceException} (409, but carries only a single
 * message, no {@code details[]}). Neither shape matches what
 * {@code docs/api/schedule.openapi.yaml} requires here - 409 *and* a field-level detail
 * per conflicting meeting - so this service adds the one exception type {@code common} is
 * missing rather than forcing the wrong status code or dropping the detail. See the
 * top-level report for the full reasoning; this is flagged there as a gap in
 * {@code common}, not silently worked around.
 */
public class MeetingConflictException extends RuntimeException {

    private final transient List<ApiError.FieldIssue> details;

    public MeetingConflictException(String message, List<ApiError.FieldIssue> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<ApiError.FieldIssue> getDetails() {
        return details;
    }
}
