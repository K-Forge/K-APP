package co.edu.konradlorenz.kapp.semaphore.web.dto;

import co.edu.konradlorenz.kapp.semaphore.domain.Program;
import co.edu.konradlorenz.kapp.semaphore.domain.ProgramLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The writable half of a program.
 *
 * <p>{@code activePensumCode} is absent on purpose. It is derived from whichever
 * pensum is currently {@code ACTIVE}, so accepting it here would let a caller put the
 * two into disagreement - see {@link Program}, which does not store it either.
 *
 * @param code institutional program code. Supplied by the caller rather than generated:
 *             it already identifies the program in the university's own records.
 */
public record ProgramRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String faculty,
        @NotNull ProgramLevel level
) {

    public Program toDomain() {
        return new Program(code, name, faculty, level);
    }
}
