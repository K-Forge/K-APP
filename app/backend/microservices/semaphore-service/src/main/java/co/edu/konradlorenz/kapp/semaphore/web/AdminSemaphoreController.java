package co.edu.konradlorenz.kapp.semaphore.web;

import co.edu.konradlorenz.kapp.semaphore.security.AdminOnly;
import co.edu.konradlorenz.kapp.semaphore.service.StudentProgressService;
import co.edu.konradlorenz.kapp.semaphore.web.dto.StudentProgressDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The administrative view of another student's semaforo.
 *
 * <p>Unlike {@code GET /api/semaphore/me}, this is a plain read: it never lazily creates
 * a document and never reconciles it against its curriculum, so it carries no
 * {@code reconciliation} block. {@code 404} therefore means either that no such user
 * exists or that the student has never opened their own semaforo yet.
 */
@RestController
@RequestMapping("/api/semaphore")
@Tag(name = "Student Progress")
public class AdminSemaphoreController {

    private final StudentProgressService progress;

    public AdminSemaphoreController(StudentProgressService progress) {
        this.progress = progress;
    }

    @GetMapping("/{userId}")
    @AdminOnly
    @Operation(summary = "Get another student's semaforo")
    public StudentProgressDto getStudentSemaphore(@PathVariable String userId) {
        return progress.getForAdmin(userId);
    }
}
