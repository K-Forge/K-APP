package co.edu.konradlorenz.kapp.common.error;

/**
 * The requested resource does not exist. Rendered as HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * @param resource the domain type, e.g. {@code Pensum}
     * @param key      the identifier that was looked up
     */
    public ResourceNotFoundException(String resource, Object key) {
        super("%s not found: %s".formatted(resource, key));
    }
}
