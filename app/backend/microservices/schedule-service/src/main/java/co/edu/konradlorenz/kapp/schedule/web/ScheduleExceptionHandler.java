package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.schedule.service.MeetingConflictException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Adds the one error mapping {@code common}'s {@code GlobalExceptionHandler} does not
 * provide: a 409 that also carries {@code details[]}. Both advices are active in the same
 * context - Spring dispatches by exception type, so this only ever handles
 * {@link MeetingConflictException} and leaves every other exception to {@code common}.
 */
@RestControllerAdvice
public class ScheduleExceptionHandler {

    @ExceptionHandler(MeetingConflictException.class)
    public ResponseEntity<ApiError> handleMeetingConflict(MeetingConflictException ex,
                                                           HttpServletRequest request) {
        ApiError body = ApiError.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                ex.getDetails());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
