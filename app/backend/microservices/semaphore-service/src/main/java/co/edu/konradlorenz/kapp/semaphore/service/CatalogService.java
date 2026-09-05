package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.common.error.ConflictException;
import co.edu.konradlorenz.kapp.common.error.DuplicateResourceException;
import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.semaphore.domain.Curriculum;
import co.edu.konradlorenz.kapp.semaphore.domain.Program;
import co.edu.konradlorenz.kapp.semaphore.repository.CurriculumRepository;
import co.edu.konradlorenz.kapp.semaphore.repository.ProgramRepository;
import co.edu.konradlorenz.kapp.semaphore.repository.StudentProgressRepository;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ProgramRequest;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ProgramResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reads and administers the academic catalog: programs and curricula.
 *
 * <p>Student progress is deliberately not this class's concern - see
 * {@code StudentProgressService}. Keeping the two apart means a catalog edit can never
 * accidentally touch a stored semaforo; every effect a curriculum change has on progress
 * documents happens lazily, through reconciliation on read.
 */
@Service
public class CatalogService {

    private final ProgramRepository programs;
    private final CurriculumRepository curricula;
    private final StudentProgressRepository progress;
    private final ActivePensumResolver activePensum;
    private final CurriculumValidator validator;

    public CatalogService(ProgramRepository programs, CurriculumRepository curricula,
                           StudentProgressRepository progress,
                           ActivePensumResolver activePensum, CurriculumValidator validator) {
        this.programs = programs;
        this.curricula = curricula;
        this.progress = progress;
        this.activePensum = activePensum;
        this.validator = validator;
    }

    public List<ProgramResponse> listPrograms() {
        return programs.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProgramResponse getProgram(String programCode) {
        Program program = programs.findById(programCode)
                .orElseThrow(() -> new ResourceNotFoundException("Program", programCode));
        return toResponse(program);
    }

    private ProgramResponse toResponse(Program program) {
        String activePensumCode = activePensum.findActive(program.code())
                .map(Curriculum::pensumCode)
                .orElse(null);
        return ProgramResponse.of(program, activePensumCode);
    }

    /**
     * @throws DuplicateResourceException (409) if the code is taken - {@code replaceProgram}
     *                                    is the way to edit an existing program
     */
    public ProgramResponse createProgram(ProgramRequest request) {
        if (programs.existsById(request.code())) {
            throw new DuplicateResourceException("Program", request.code());
        }
        return toResponse(programs.save(request.toDomain()));
    }

    /**
     * @throws BusinessRuleException     (400) if the body's {@code code} does not match the path.
     *                                    The path wins; the code is the program's identity and
     *                                    accepting a different one in the body would silently
     *                                    edit a different program than the URL names
     * @throws ResourceNotFoundException (404) if no such program exists yet
     */
    public ProgramResponse replaceProgram(String programCode, ProgramRequest request) {
        if (!programCode.equals(request.code())) {
            throw new BusinessRuleException("code in the body must match the path",
                    List.of(new ApiError.FieldIssue(
                            "code", "must equal the path parameter '%s'".formatted(programCode))));
        }
        if (!programs.existsById(programCode)) {
            throw new ResourceNotFoundException("Program", programCode);
        }
        return toResponse(programs.save(request.toDomain()));
    }

    /**
     * Deleting never cascades.
     *
     * @throws ResourceNotFoundException (404) if no such program exists
     * @throws ConflictException         (409) while any curriculum still references it. The
     *                                    blocking pensum codes are named in the response:
     *                                    "something is using it" is not actionable, and a
     *                                    silent cascade would orphan every student following
     *                                    those curricula
     */
    public void deleteProgram(String programCode) {
        if (!programs.existsById(programCode)) {
            throw new ResourceNotFoundException("Program", programCode);
        }
        List<Curriculum> dependent = curricula.findByProgramCode(programCode);
        if (!dependent.isEmpty()) {
            List<String> codes = dependent.stream().map(Curriculum::pensumCode).sorted().toList();
            throw new ConflictException(
                    "Program %s still has %d curriculum(s) and cannot be deleted"
                            .formatted(programCode, codes.size()),
                    codes.stream()
                            .map(code -> new ApiError.FieldIssue("pensumCode", code))
                            .toList());
        }
        programs.deleteById(programCode);
    }

    public CurriculumDto getCurriculum(String pensumCode) {
        return CurriculumDto.from(requireCurriculum(pensumCode));
    }

    public List<CurriculumCourseDto> listCurriculumCourses(String pensumCode, Integer level,
                                                            String area, Boolean isElectiveSlot) {
        Curriculum curriculum = requireCurriculum(pensumCode);
        return curriculum.coursesInDisplayOrder().stream()
                .filter(course -> level == null || course.level() == level)
                .filter(course -> area == null || area.equals(course.area()))
                .filter(course -> isElectiveSlot == null || isElectiveSlot == course.electiveSlot())
                .map(CurriculumCourseDto::from)
                .toList();
    }

    /**
     * @throws DuplicateResourceException (409) if {@code pensumCode} already exists -
     *                                    {@code replaceCurriculum} is the way to edit it
     */
    public CurriculumDto createCurriculum(CurriculumDto dto) {
        validator.validate(dto);
        if (curricula.existsById(dto.pensumCode())) {
            throw new DuplicateResourceException("Curriculum", dto.pensumCode());
        }
        return CurriculumDto.from(curricula.save(dto.toDomain()));
    }

    /**
     * @throws BusinessRuleException     (400) if the body's {@code pensumCode} does not
     *                                    match the path
     * @throws ResourceNotFoundException (404) if no such curriculum exists yet -
     *                                    {@code createCurriculum} is the way to add it
     */
    public CurriculumDto replaceCurriculum(String pensumCode, CurriculumDto dto) {
        if (!pensumCode.equals(dto.pensumCode())) {
            throw new BusinessRuleException("pensumCode in the body must match the path",
                    List.of(new ApiError.FieldIssue(
                            "pensumCode", "must equal the path parameter '%s'".formatted(pensumCode))));
        }
        if (!curricula.existsById(pensumCode)) {
            throw new ResourceNotFoundException("Curriculum", pensumCode);
        }
        validator.validate(dto);
        return CurriculumDto.from(curricula.save(dto.toDomain()));
    }

    /**
     * Deleting never cascades.
     *
     * @throws ResourceNotFoundException (404) if no such curriculum exists
     * @throws ConflictException         (409) while any student is still pinned to it -
     *                                    deleting it would leave their semaforo pointing at a
     *                                    pensum that no longer exists, and the reconciler has
     *                                    no way to recover from that
     */
    public void deleteCurriculum(String pensumCode) {
        if (!curricula.existsById(pensumCode)) {
            throw new ResourceNotFoundException("Curriculum", pensumCode);
        }
        // A count, not the student ids: who follows a pensum is not the caller's business,
        // and an administrator only needs to know that somebody does.
        long followers = progress.countByPensumCode(pensumCode);
        if (followers > 0) {
            throw new ConflictException(
                    "Curriculum %s is still followed by %d student(s) and cannot be deleted"
                            .formatted(pensumCode, followers),
                    List.of(new ApiError.FieldIssue("students", String.valueOf(followers))));
        }
        curricula.deleteById(pensumCode);
    }

    private Curriculum requireCurriculum(String pensumCode) {
        return curricula.findById(pensumCode)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum", pensumCode));
    }
}
