package co.edu.konradlorenz.kapp.semaphore.web.dto;

import co.edu.konradlorenz.kapp.semaphore.domain.Program;
import co.edu.konradlorenz.kapp.semaphore.domain.ProgramLevel;

/**
 * A program as the catalogue returns it.
 *
 * @param activePensumCode code of the program's ACTIVE pensum, or null when its only
 *                         pensums are DRAFT or OBSOLETE. Derived at read time, never
 *                         stored on the program - see {@link Program}.
 */
public record ProgramResponse(
        String code,
        String name,
        String faculty,
        ProgramLevel level,
        String activePensumCode
) {

    public static ProgramResponse of(Program program, String activePensumCode) {
        return new ProgramResponse(program.code(), program.name(), program.faculty(),
                program.level(), activePensumCode);
    }
}
