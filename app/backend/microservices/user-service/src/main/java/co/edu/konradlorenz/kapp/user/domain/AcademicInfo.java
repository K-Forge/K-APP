package co.edu.konradlorenz.kapp.user.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Academic placement of a member of the university.
 *
 * <p><strong>Null for every {@code ROLE_GUEST} account.</strong> A guest has no student
 * code, no programme and no pensum, so nothing anywhere may assume this object is
 * present. The contract makes the same promise to the mobile clients.
 *
 * @param studentCode  university student code
 * @param programCode  programme code; {@code 506} is Ingenieria de Sistemas
 * @param pensumCode   curriculum version the student is bound to. Not derivable from the
 *                     programme: a student who enrolled under an earlier curriculum keeps
 *                     the pensum they started on
 * @param currentLevel semester currently enrolled in, 1-12 per the contract
 */
public record AcademicInfo(

        @NotNull(message = "must not be null")
        @Pattern(regexp = "^\\d{6,20}$", message = "must be 6 to 20 digits")
        String studentCode,

        @NotNull(message = "must not be null")
        @Pattern(regexp = "^\\d{1,10}$", message = "must be 1 to 10 digits")
        String programCode,

        @NotNull(message = "must not be null")
        @Pattern(regexp = "^\\d{1,10}$", message = "must be 1 to 10 digits")
        String pensumCode,

        @NotNull(message = "must not be null")
        @Min(value = 1, message = "must be at least 1")
        @Max(value = 12, message = "must be at most 12")
        Integer currentLevel
) {
}
