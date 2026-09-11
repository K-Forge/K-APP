package co.edu.konradlorenz.kapp.user.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Identity document of a user, or {@code null} while they have not supplied one.
 *
 * <p>The number is a string, never a number: leading zeros are significant on a Colombian
 * cedula and a passport carries letters. Parsing it as an integer loses both.
 *
 * @param type   document class
 * @param number document number as printed, 5-20 characters per the contract
 */
public record Identification(

        @NotNull(message = "must not be null")
        IdentificationType type,

        @NotNull(message = "must not be null")
        @Size(min = 5, max = 20, message = "must be between 5 and 20 characters")
        String number
) {
}
