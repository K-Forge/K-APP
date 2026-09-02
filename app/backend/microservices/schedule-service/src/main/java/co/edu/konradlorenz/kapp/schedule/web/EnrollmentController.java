package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.common.security.CurrentUser;
import co.edu.konradlorenz.kapp.common.security.KappRoles;
import co.edu.konradlorenz.kapp.schedule.mapper.ScheduleMapper;
import co.edu.konradlorenz.kapp.schedule.service.EnrollmentService;
import co.edu.konradlorenz.kapp.schedule.web.dto.CreateEnrollmentRequest;
import co.edu.konradlorenz.kapp.schedule.web.dto.EnrollmentResponse;
import co.edu.konradlorenz.kapp.schedule.web.dto.UpdateEnrollmentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Courses inside the caller's active schedule. */
@RestController
@RequestMapping("/api/schedule/me/enrollments")
@PreAuthorize("hasAnyRole('" + KappRoles.Short.STUDENT + "', '" + KappRoles.Short.PROFESSOR
        + "', '" + KappRoles.Short.ADMIN + "')")
@Tag(name = "Enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final EnrollmentPatchReader patchReader;

    public EnrollmentController(EnrollmentService enrollmentService, EnrollmentPatchReader patchReader) {
        this.enrollmentService = enrollmentService;
        this.patchReader = patchReader;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a course to the caller's schedule",
            description = "Every meeting is checked against the rest of the schedule and against "
                    + "the other meetings in this same request; on conflict nothing is written.")
    public EnrollmentResponse addEnrollment(@Valid @RequestBody CreateEnrollmentRequest request) {
        return ScheduleMapper.toResponse(enrollmentService.addEnrollment(CurrentUser.id(), request));
    }

    /**
     * Binds the raw JSON tree, like {@code UserProfileController.updateMe}: a record
     * cannot tell "the client left this alone" from "the client cleared it", and only
     * {@code subgroup} is nullable here, so the distinction matters even for a single field.
     */
    @PatchMapping(path = "/{enrollmentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Partially update an enrollment",
            description = "Meetings are not editable here; sending a meetings key is rejected with 400.")
    public EnrollmentResponse updateEnrollment(@PathVariable String enrollmentId,
                                               @RequestBody JsonNode body) {
        UpdateEnrollmentRequest patch = patchReader.read(body);
        return ScheduleMapper.toResponse(
                enrollmentService.updateEnrollment(CurrentUser.id(), enrollmentId, patch));
    }

    @DeleteMapping("/{enrollmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a course from the caller's schedule",
            description = "Deletes the enrollment together with all of its meetings.")
    public void deleteEnrollment(@PathVariable String enrollmentId) {
        enrollmentService.deleteEnrollment(CurrentUser.id(), enrollmentId);
    }
}
