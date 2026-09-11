package co.edu.konradlorenz.kapp.semaphore.web;

import co.edu.konradlorenz.kapp.common.security.CurrentUser;
import co.edu.konradlorenz.kapp.semaphore.security.StudentOnly;
import co.edu.konradlorenz.kapp.semaphore.service.StudentProgressService;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CourseProgressUpdateRequest;
import co.edu.konradlorenz.kapp.semaphore.web.dto.PensumCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ElectiveResolutionRequest;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ProgressSummaryDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.StudentProgressCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.StudentProgressWithReconciliationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The authenticated student's own semaforo.
 *
 * <p>{@code ROLE_STUDENT} only - see {@link StudentOnly}. Identity always comes from
 * {@link CurrentUser#id()}, the validated JWT subject, never from a path parameter: that
 * is what makes "my own document" an invariant rather than something a client could
 * spoof by passing a different id.
 */
@RestController
@RequestMapping("/api/semaphore/me")
@Tag(name = "Student Progress")
public class StudentProgressController {

    private final StudentProgressService progress;

    public StudentProgressController(StudentProgressService progress) {
        this.progress = progress;
    }

    @GetMapping
    @StudentOnly
    @Operation(summary = "Get the caller's semaforo",
            description = "Lazily created on first call. Never 404s for a student with a program.")
    public StudentProgressWithReconciliationDto getMine() {
        return progress.getMineWithReconciliation(CurrentUser.id());
    }

    @GetMapping("/summary")
    @StudentOnly
    @Operation(summary = "Get the caller's progress summary")
    public ProgressSummaryDto getSummary() {
        return progress.getSummary(CurrentUser.id());
    }

    @GetMapping("/eligible")
    @StudentOnly
    @Operation(summary = "List the courses the caller can take next")
    public List<PensumCourseDto> getEligible() {
        return progress.getEligible(CurrentUser.id());
    }

    @PutMapping("/courses/{code}")
    @StudentOnly
    @Operation(summary = "Set the status of one course on the caller's semaforo")
    public StudentProgressCourseDto updateCourseStatus(@PathVariable String code,
                                                        @Valid @RequestBody CourseProgressUpdateRequest body) {
        return progress.updateCourseStatus(CurrentUser.id(), code, body);
    }

    @PostMapping("/electives/{pensumItemCode}")
    @StudentOnly
    @Operation(summary = "Resolve an elective slot to a real course")
    public StudentProgressCourseDto resolveElective(@PathVariable String pensumItemCode,
                                                     @Valid @RequestBody ElectiveResolutionRequest body) {
        return progress.resolveElective(CurrentUser.id(), pensumItemCode, body);
    }

    @DeleteMapping("/electives/{pensumItemCode}")
    @StudentOnly
    @Operation(summary = "Clear an elective slot resolution")
    public ResponseEntity<Void> clearElective(@PathVariable String pensumItemCode) {
        progress.clearElective(CurrentUser.id(), pensumItemCode);
        return ResponseEntity.noContent().build();
    }
}
