package co.edu.konradlorenz.kapp.user.domain;

/**
 * Class of Colombian identity document, exactly as the contract enumerates it.
 *
 * <p>{@code CC} cedula de ciudadania, {@code TI} tarjeta de identidad, {@code CE} cedula
 * de extranjeria, {@code PASAPORTE} passport.
 */
public enum IdentificationType {
    CC,
    TI,
    CE,
    PASAPORTE
}
