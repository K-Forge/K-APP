package co.edu.konradlorenz.kapp.common.error;

/**
 * Authentication failed. Rendered as HTTP 401.
 *
 * <p>The message must stay deliberately vague. Distinguishing "no such account" from
 * "wrong password" turns the login endpoint into an account enumeration oracle, letting
 * an attacker confirm which university e-mails are registered.
 */
public class InvalidCredentialsException extends RuntimeException {

    private static final String SAFE_MESSAGE = "Invalid e-mail or password";

    public InvalidCredentialsException() {
        super(SAFE_MESSAGE);
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
