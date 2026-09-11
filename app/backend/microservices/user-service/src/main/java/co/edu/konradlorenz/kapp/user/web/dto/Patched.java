package co.edu.konradlorenz.kapp.user.web.dto;

/**
 * One field of a PATCH body, carrying the distinction a plain value cannot.
 *
 * <p>A partial update has three states per field, not two: absent means <em>leave it
 * alone</em>, an explicit {@code null} means <em>clear it</em>, and a value means
 * <em>set it</em>. Binding the body to an ordinary record collapses the first two into
 * the same {@code null} field, so either every omitted field wipes the profile or no
 * field can ever be cleared. Neither is what the contract promises.
 *
 * @param present whether the client sent the field at all
 * @param value   what they sent, possibly null
 */
public record Patched<T>(boolean present, T value) {

    private static final Patched<?> ABSENT = new Patched<>(false, null);

    @SuppressWarnings("unchecked")
    public static <T> Patched<T> absent() {
        return (Patched<T>) ABSENT;
    }

    public static <T> Patched<T> of(T value) {
        return new Patched<>(true, value);
    }

    /** @return the new value when the field was sent, otherwise what the profile holds */
    public T orElse(T current) {
        return present ? value : current;
    }

    /** @return true when the client sent an actual value, as opposed to null or nothing */
    public boolean hasValue() {
        return present && value != null;
    }
}
