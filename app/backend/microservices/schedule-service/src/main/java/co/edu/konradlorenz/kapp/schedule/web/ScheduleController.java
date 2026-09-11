package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.common.academic.ValidAcademicPeriod;
import co.edu.konradlorenz.kapp.common.security.CurrentUser;
import co.edu.konradlorenz.kapp.common.security.KappRoles;
import co.edu.konradlorenz.kapp.schedule.mapper.ScheduleMapper;
import co.edu.konradlorenz.kapp.schedule.service.ScheduleService;
import co.edu.konradlorenz.kapp.schedule.web.dto.CreateScheduleRequest;
import co.edu.konradlorenz.kapp.schedule.web.dto.SchedulePeriodSummaryResponse;
import co.edu.konradlorenz.kapp.schedule.web.dto.ScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The schedule container itself: create, read and delete one period's timetable, and list
 * which periods the caller has a timetable for.
 *
 * <p>{@code ROLE_GUEST} is forbidden here, per the contract's own "guests may only read the
 * campus map" rule - the class-level {@code @PreAuthorize} is the one place that is
 * enforced, so it cannot be forgotten on a future method.
 */
@RestController
@RequestMapping("/api/schedule")
@PreAuthorize("hasAnyRole('" + KappRoles.Short.STUDENT + "', '" + KappRoles.Short.PROFESSOR
        + "', '" + KappRoles.Short.ADMIN + "')")
@Validated
@Tag(name = "Schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the caller's schedule for a period",
            description = "When period is omitted the active schedule is returned.")
    public ScheduleResponse getMySchedule(
            @RequestParam(required = false) @ValidAcademicPeriod String period) {
        return ScheduleMapper.toResponse(scheduleService.getSchedule(CurrentUser.id(), period));
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create the caller's schedule for a period",
            description = "A student may hold at most one schedule per period.")
    public ScheduleResponse createMySchedule(@Valid @RequestBody CreateScheduleRequest request) {
        return ScheduleMapper.toResponse(scheduleService.createSchedule(CurrentUser.id(), request));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete the caller's schedule for a period",
            description = "period is required: there is no delete-whatever-is-active shortcut.")
    public void deleteMySchedule(
            @RequestParam(required = false) @NotBlank @ValidAcademicPeriod String period) {
        scheduleService.deleteSchedule(CurrentUser.id(), period);
    }

    @GetMapping("/me/periods")
    @Operation(summary = "List the periods the caller has a schedule for",
            description = "One entry per stored schedule, newest period first.")
    public List<SchedulePeriodSummaryResponse> listMySchedulePeriods() {
        return scheduleService.listPeriods(CurrentUser.id());
    }
}
