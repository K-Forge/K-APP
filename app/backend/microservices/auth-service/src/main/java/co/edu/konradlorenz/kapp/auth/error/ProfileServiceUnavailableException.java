package co.edu.konradlorenz.kapp.auth.error;

/**
 * user-service could not be reached, or answered with something registration cannot use.
 *
 * <p>Rendered as HTTP 503 rather than 500, because the distinction matters to a client:
 * 503 says "this will probably work if you try again", and nothing the caller sent was
 * wrong. The message is deliberately free of any detail about which service failed or why.
 *
 * <p>Local to auth-service on purpose. {@code common} carries the error types every
 * service shares; a dependency being down is specific to the one service that has a
 * dependency.
 */
public class ProfileServiceUnavailableException extends RuntimeException {

    public ProfileServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
