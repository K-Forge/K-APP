package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.semaphore.domain.Curriculum;
import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumStatus;
import co.edu.konradlorenz.kapp.semaphore.repository.CurriculumRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * The single place that turns a {@code programCode} into its {@code ACTIVE} pensum.
 *
 * <p>Two call sites need exactly this lookup: the program catalog, to report
 * {@code activePensumCode} on every {@code Program}, and lazy creation of
 * {@code GET /api/semaphore/me}, to pick the curriculum a fresh semaforo is pinned to.
 * Both go through here so the definition of "active" - and its failure mode when a
 * program has none - can never drift between the two.
 */
@Component
public class ActivePensumResolver {

    private final CurriculumRepository curricula;

    public ActivePensumResolver(CurriculumRepository curricula) {
        this.curricula = curricula;
    }

    /**
     * @return the program's {@code ACTIVE} curriculum, or empty if it has none. Used
     *         where "no active pensum" is a legitimate, non-exceptional answer, such as
     *         listing the catalog.
     */
    public Optional<Curriculum> findActive(String programCode) {
        return curricula.findFirstByProgramCodeAndStatus(programCode, CurriculumStatus.ACTIVE);
    }

    /**
     * @return the program's {@code ACTIVE} curriculum
     * @throws ResourceNotFoundException if the program has none - the one case
     *                                   {@code GET /api/semaphore/me} answers with 404
     */
    public Curriculum resolveActiveOrThrow(String programCode) {
        return findActive(programCode)
                .orElseThrow(() -> new ResourceNotFoundException("Active curriculum for program", programCode));
    }
}
