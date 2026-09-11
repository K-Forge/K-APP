package co.edu.konradlorenz.kapp.auth.domain;

/**
 * The kinds of identity document a visitor may present at reception, as used in Colombia.
 *
 * <p>Stored rather than inferred from the number's shape: a {@code TI} and a {@code CC} can
 * carry the same digits for the same person at different ages, and telling reception which
 * document they were shown is the point of keeping the record at all.
 */
public enum DocumentType {
    /** Cédula de ciudadanía. */
    CC,
    /** Cédula de extranjería. */
    CE,
    /** Tarjeta de identidad, for a visitor under 18. */
    TI,
    /** Pasaporte. */
    PASAPORTE,
    /** Permiso por Protección Temporal. */
    PPT
}
