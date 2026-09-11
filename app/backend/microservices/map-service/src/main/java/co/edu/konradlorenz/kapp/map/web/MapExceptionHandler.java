package co.edu.konradlorenz.kapp.map.web;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.map.service.MapConflictException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders map-specific conflicts in the shared {@link ApiError} envelope.
 *
 * <p>Everything else - validation, 404, 403, the catch-all 500 - is handled by
 * {@code common}'s {@code GlobalExceptionHandler}, which this advice does not duplicate or
 * override. It exists only because the contract requires the candidate buildings of an
 * ambiguous room code to appear in {@code details}, and the shared 409 exception carries no
 * details.
 *
 * <p>Ordered ahead of the shared advice so its {@code Exception} catch-all cannot claim
 * these first.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MapExceptionHandler {

    @ExceptionHandler(MapConflictException.class)
    public ResponseEntity<ApiError> handleConflict(MapConflictException ex,
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
