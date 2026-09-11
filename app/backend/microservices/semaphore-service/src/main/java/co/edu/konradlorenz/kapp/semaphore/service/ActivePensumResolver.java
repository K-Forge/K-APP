package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.semaphore.domain.Pensum;
import co.edu.konradlorenz.kapp.semaphore.domain.PensumStatus;
import co.edu.konradlorenz.kapp.semaphore.repository.PensumRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * The single place that turns a {@code programCode} into its {@code ACTIVE} pensum.
 *
 * <p>Two call sites need exactly this lookup: the program catalog, to report
 * {@code activePensumCode} on every {@code Program}, and lazy creation of
 * {@code GET /api/semaphore/me}, to pick the pensum a fresh semaforo is pinned to.
 * Both go through here so the definition of "active" - and its failure mode when a
 * program has none - can never drift between the two.
 */
@Component
public class ActivePensumResolver {

    private final PensumRepository pensums;

    public ActivePensumResolver(PensumRepository pensums) {
        this.pensums = pensums;
    }

    /**
     * @return the program's {@code ACTIVE} pensum, or empty if it has none. Used
     *         where "no active pensum" is a legitimate, non-exceptional answer, such as
     *         listing the catalog.
     */
    public Optional<Pensum> findActive(String programCode) {
        return pensums.findFirstByProgramCodeAndStatus(programCode, PensumStatus.ACTIVE);
    }

    /**
     * @return the program's {@code ACTIVE} pensum
     * @throws ResourceNotFoundException if the program has none - the one case
     *                                   {@code GET /api/semaphore/me} answers with 404
     */
    public Pensum resolveActiveOrThrow(String programCode) {
        return findActive(programCode)
                .orElseThrow(() -> new ResourceNotFoundException("Active pensum for program", programCode));
    }
}
