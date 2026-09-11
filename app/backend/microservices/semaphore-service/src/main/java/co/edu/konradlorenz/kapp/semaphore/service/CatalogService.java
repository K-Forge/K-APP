package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.common.error.ConflictException;
import co.edu.konradlorenz.kapp.common.error.DuplicateResourceException;
import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.semaphore.domain.Pensum;
import co.edu.konradlorenz.kapp.semaphore.domain.PensumStatus;
import co.edu.konradlorenz.kapp.semaphore.domain.Program;
import co.edu.konradlorenz.kapp.semaphore.repository.PensumRepository;
import co.edu.konradlorenz.kapp.semaphore.repository.ProgramRepository;
import co.edu.konradlorenz.kapp.semaphore.repository.StudentProgressRepository;
import co.edu.konradlorenz.kapp.semaphore.web.dto.PensumCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.PensumDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.PensumSummary;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ProgramRequest;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ProgramResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Reads and administers the academic catalog: programs and pensums.
 *
 * <p>Student progress is deliberately not this class's concern - see
 * {@code StudentProgressService}. Keeping the two apart means a catalog edit can never
 * accidentally touch a stored semaforo; every effect a pensum change has on progress
 * documents happens lazily, through reconciliation on read.
 */
@Service
public class CatalogService {

    private final ProgramRepository programs;
    private final PensumRepository pensums;
    private final StudentProgressRepository progress;
    private final ActivePensumResolver activePensum;
    private final PensumValidator validator;

    public CatalogService(ProgramRepository programs, PensumRepository pensums,
                           StudentProgressRepository progress,
                           ActivePensumResolver activePensum, PensumValidator validator) {
        this.programs = programs;
        this.pensums = pensums;
        this.progress = progress;
        this.activePensum = activePensum;
        this.validator = validator;
    }

    public List<ProgramResponse> listPrograms() {
        return programs.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Every pensum, newest-looking first: active ones before drafts, then by code.
     *
     * <p>Summaries, not documents - see {@link PensumSummary}. The whole catalogue is a few
     * dozen rows, so this is deliberately not paginated: a picker that pages is a picker nobody
     * can use, and the day this needs pages is the day it needs a search box instead.
     */
    public List<PensumSummary> listPensums() {
        return pensums.findAll().stream()
                .sorted(Comparator
                        .comparing((Pensum c) -> c.status() != PensumStatus.ACTIVE)
                        .thenComparing(Pensum::programName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(Pensum::pensumCode))
                .map(PensumSummary::from)
                .toList();
    }

    public ProgramResponse getProgram(String programCode) {
        Program program = programs.findById(programCode)
                .orElseThrow(() -> new ResourceNotFoundException("Program", programCode));
        return toResponse(program);
    }

    private ProgramResponse toResponse(Program program) {
        String activePensumCode = activePensum.findActive(program.code())
                .map(Pensum::pensumCode)
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
     * @throws ConflictException         (409) while any pensum still references it. The
     *                                    blocking pensum codes are named in the response:
     *                                    "something is using it" is not actionable, and a
     *                                    silent cascade would orphan every student following
     *                                    those pensums
     */
    public void deleteProgram(String programCode) {
        if (!programs.existsById(programCode)) {
            throw new ResourceNotFoundException("Program", programCode);
        }
        List<Pensum> dependent = pensums.findByProgramCode(programCode);
        if (!dependent.isEmpty()) {
            List<String> codes = dependent.stream().map(Pensum::pensumCode).sorted().toList();
            // "pensum", not "pensum": that is what the building calls it, what the portal
            // calls it, and this string is read by a person deciding what to do next. And
            // "1 pensum" rather than "1 pensum(s)", because a message that cannot be bothered
            // to pluralise reads like nobody expected anyone to see it.
            throw new ConflictException(
                    "Program %s still has %d %s and cannot be deleted"
                            .formatted(programCode, codes.size(),
                                    codes.size() == 1 ? "pensum" : "pensums"),
                    codes.stream()
                            .map(code -> new ApiError.FieldIssue("pensumCode", code))
                            .toList());
        }
        programs.deleteById(programCode);
    }

    public PensumDto getPensum(String pensumCode) {
        return PensumDto.from(requirePensum(pensumCode));
    }

    public List<PensumCourseDto> listPensumCourses(String pensumCode, Integer level,
                                                            String area, Boolean isElectiveSlot) {
        Pensum pensum = requirePensum(pensumCode);
        return pensum.coursesInDisplayOrder().stream()
                .filter(course -> level == null || course.level() == level)
                .filter(course -> area == null || area.equals(course.area()))
                .filter(course -> isElectiveSlot == null || isElectiveSlot == course.electiveSlot())
                .map(PensumCourseDto::from)
                .toList();
    }

    /**
     * @throws DuplicateResourceException (409) if {@code pensumCode} already exists -
     *                                    {@code replacePensum} is the way to edit it
     */
    public PensumDto createPensum(PensumDto dto) {
        validator.validate(dto);
        if (pensums.existsById(dto.pensumCode())) {
            throw new DuplicateResourceException("Pensum", dto.pensumCode());
        }
        return PensumDto.from(pensums.save(dto.toDomain()));
    }

    /**
     * @throws BusinessRuleException     (400) if the body's {@code pensumCode} does not
     *                                    match the path
     * @throws ResourceNotFoundException (404) if no such pensum exists yet -
     *                                    {@code createPensum} is the way to add it
     */
    public PensumDto replacePensum(String pensumCode, PensumDto dto) {
        if (!pensumCode.equals(dto.pensumCode())) {
            throw new BusinessRuleException("pensumCode in the body must match the path",
                    List.of(new ApiError.FieldIssue(
                            "pensumCode", "must equal the path parameter '%s'".formatted(pensumCode))));
        }
        if (!pensums.existsById(pensumCode)) {
            throw new ResourceNotFoundException("Pensum", pensumCode);
        }
        validator.validate(dto);
        return PensumDto.from(pensums.save(dto.toDomain()));
    }

    /**
     * Deleting never cascades.
     *
     * @throws ResourceNotFoundException (404) if no such pensum exists
     * @throws ConflictException         (409) while any student is still pinned to it -
     *                                    deleting it would leave their semaforo pointing at a
     *                                    pensum that no longer exists, and the reconciler has
     *                                    no way to recover from that
     */
    public void deletePensum(String pensumCode) {
        if (!pensums.existsById(pensumCode)) {
            throw new ResourceNotFoundException("Pensum", pensumCode);
        }
        // A count, not the student ids: who follows a pensum is not the caller's business,
        // and an administrator only needs to know that somebody does.
        long followers = progress.countByPensumCode(pensumCode);
        if (followers > 0) {
            throw new ConflictException(
                    "Pensum %s is still followed by %d student(s) and cannot be deleted"
                            .formatted(pensumCode, followers),
                    List.of(new ApiError.FieldIssue("students", String.valueOf(followers))));
        }
        pensums.deleteById(pensumCode);
    }

    private Pensum requirePensum(String pensumCode) {
        return pensums.findById(pensumCode)
                .orElseThrow(() -> new ResourceNotFoundException("Pensum", pensumCode));
    }
}
