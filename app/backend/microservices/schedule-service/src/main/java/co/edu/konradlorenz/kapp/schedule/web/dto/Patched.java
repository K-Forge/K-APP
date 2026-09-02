package co.edu.konradlorenz.kapp.schedule.web.dto;

/**
 * One field of a PATCH body. Mirrors {@code user-service}'s {@code Patched<T>} exactly:
 * a partial update has three states per field, not two.
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

    /** @return the new value when the field was sent, otherwise what is already stored */
    public T orElse(T current) {
        return present ? value : current;
    }
}
