package co.edu.konradlorenz.kapp.common.error;

/**
 * The request conflicts with something that already exists, such as registering an
 * e-mail that is already taken. Rendered as HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    /**
     * @param resource the domain type, e.g. {@code Credential}
     * @param key      the identifier that already exists
     */
    public DuplicateResourceException(String resource, Object key) {
        super("%s already exists: %s".formatted(resource, key));
    }
}
