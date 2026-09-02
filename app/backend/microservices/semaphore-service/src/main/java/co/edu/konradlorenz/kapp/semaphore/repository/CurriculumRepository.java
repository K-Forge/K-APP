package co.edu.konradlorenz.kapp.semaphore.repository;

import co.edu.konradlorenz.kapp.semaphore.domain.Curriculum;
import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CurriculumRepository extends MongoRepository<Curriculum, String> {

    /**
     * The lookup that lazy creation depends on: a program's current plan.
     *
     * <p>Returns an {@link Optional} rather than a single value because a program whose
     * only curricula are DRAFT or OBSOLETE legitimately has none, and that is the one case
     * {@code GET /api/semaphore/me} is allowed to answer with 404.
     */
    Optional<Curriculum> findFirstByProgramCodeAndStatus(String programCode, CurriculumStatus status);

    List<Curriculum> findByProgramCode(String programCode);
}
