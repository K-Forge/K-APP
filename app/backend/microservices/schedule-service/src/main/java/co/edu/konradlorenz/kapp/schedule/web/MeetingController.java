package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.common.security.CurrentUser;
import co.edu.konradlorenz.kapp.common.security.KappRoles;
import co.edu.konradlorenz.kapp.schedule.mapper.ScheduleMapper;
import co.edu.konradlorenz.kapp.schedule.service.MeetingService;
import co.edu.konradlorenz.kapp.schedule.web.dto.CreateMeetingRequest;
import co.edu.konradlorenz.kapp.schedule.web.dto.MeetingResponse;
import co.edu.konradlorenz.kapp.schedule.web.dto.UpdateMeetingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Weekly day + time slots inside one enrollment. */
@RestController
@RequestMapping("/api/schedule/me/enrollments/{enrollmentId}/meetings")
@PreAuthorize("hasAnyRole('" + KappRoles.Short.STUDENT + "', '" + KappRoles.Short.PROFESSOR
        + "', '" + KappRoles.Short.ADMIN + "')")
@Tag(name = "Meetings")
public class MeetingController {

    private final MeetingService meetingService;
    private final MeetingPatchReader patchReader;

    public MeetingController(MeetingService meetingService, MeetingPatchReader patchReader) {
        this.meetingService = meetingService;
        this.patchReader = patchReader;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a weekly slot to an enrollment",
            description = "Checked against every other meeting in the caller's schedule; "
                    + "on conflict nothing is written.")
    public MeetingResponse addMeeting(@PathVariable String enrollmentId,
                                      @Valid @RequestBody CreateMeetingRequest request) {
        return ScheduleMapper.toResponse(
                meetingService.addMeeting(CurrentUser.id(), enrollmentId, request));
    }

    @PatchMapping(path = "/{meetingId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Partially update a weekly slot",
            description = "periods, when present, replaces the whole list. Re-checked for "
                    + "conflicts against the rest of the schedule.")
    public MeetingResponse updateMeeting(@PathVariable String enrollmentId,
                                         @PathVariable String meetingId,
                                         @RequestBody JsonNode body) {
        UpdateMeetingRequest patch = patchReader.read(body);
        return ScheduleMapper.toResponse(
                meetingService.updateMeeting(CurrentUser.id(), enrollmentId, meetingId, patch));
    }

    @DeleteMapping("/{meetingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a weekly slot from an enrollment",
            description = "The enrollment itself is kept even if this was its last meeting.")
    public void deleteMeeting(@PathVariable String enrollmentId, @PathVariable String meetingId) {
        meetingService.deleteMeeting(CurrentUser.id(), enrollmentId, meetingId);
    }
}
