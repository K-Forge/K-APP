package co.edu.konradlorenz.kapp.semaphore.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * An academic program offered by the university.
 *
 * <p>The program does NOT carry its active pensum code as a field. That would be a second
 * place to write the same fact, and the two would drift the first time an administrator
 * activated a new curriculum without remembering to update the program. The active pensum
 * is derived by querying {@code curricula} for this program's {@link CurriculumStatus#ACTIVE}
 * plan, which is what the unique-per-program invariant is for.
 *
 * @param code    institutional program code, e.g. {@code "506"}
 * @param name    display name, e.g. {@code "Ingenieria de Sistemas"}
 * @param faculty name of the owning faculty
 * @param level   academic level
 */
@Document(collection = "programs")
public record Program(
        @Id String code,
        String name,
        String faculty,
        ProgramLevel level
) {
}
