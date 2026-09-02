package co.edu.konradlorenz.kapp.semaphore.domain;

/**
 * Academic level of a program, mirroring the university's {@code program_level_enum}.
 *
 * <p>Kept verbatim from the legacy schema so a future synchronisation with SINU needs no
 * translation table.
 */
public enum ProgramLevel {
    PREGRADO,
    POSGRADO,
    TECNOLOGIA,
    MAESTRIA,
    DOCTORADO,
    CURSOS_DIPLOMADOS,
    ESPECIALIZACION
}
