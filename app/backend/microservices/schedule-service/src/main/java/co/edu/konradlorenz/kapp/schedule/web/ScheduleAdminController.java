package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.common.academic.ValidAcademicPeriod;
import co.edu.konradlorenz.kapp.common.security.KappRoles;
import co.edu.konradlorenz.kapp.schedule.mapper.ScheduleMapper;
import co.edu.konradlorenz.kapp.schedule.service.ScheduleService;
import co.edu.konradlorenz.kapp.schedule.web.dto.ScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative read access to another student's schedule. {@code ROLE_ADMIN} only -
 * every other role, including a student asking for their own numeric id, is refused with
 * 403; students use the {@code /me} family in {@link ScheduleController} instead.
 *
 * <p>The literal path {@code /api/schedule/me} in {@link ScheduleController} always wins
 * over this controller's {@code {userId}} template, so {@code me} can never be read as an
 * id - Spring Security and Spring MVC both prefer an exact path match over a variable one.
 *
 * <p>{@code userId} is bound as a plain string, not the contract's {@code integer/int64}:
 * see the top-level report for why - every id on this platform, including the
 * {@code sub} claim {@link co.edu.konradlorenz.kapp.common.security.CurrentUser#id()}
 * resolves, is an opaque string, never numeric.
 */
@RestController
@RequestMapping("/api/schedule")
@PreAuthorize("hasRole('" + KappRoles.Short.ADMIN + "')")
@Validated
@Tag(name = "Admin")
public class ScheduleAdminController {

    private final ScheduleService scheduleService;

    public ScheduleAdminController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get any student's schedule",
            description = "Same payload as GET /api/schedule/me. ROLE_ADMIN only.")
    public ScheduleResponse getScheduleByUserId(@PathVariable String userId,
            @RequestParam(required = false) @ValidAcademicPeriod String period) {
        return ScheduleMapper.toResponse(scheduleService.getScheduleForAdmin(userId, period));
    }
}
