package co.edu.konradlorenz.kapp.schedule.web.dto;

import co.edu.konradlorenz.kapp.common.academic.ValidAcademicPeriod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body of {@code POST /api/schedule/me}. */
public record CreateScheduleRequest(

        @NotBlank(message = "must not be blank")
        @ValidAcademicPeriod
        String period,

        @NotBlank(message = "must not be blank")
        String programCode,

        @NotBlank(message = "must not be blank")
        String pensumCode,

        @NotNull(message = "must not be null")
        @Min(value = 1, message = "must be at least 1")
        @Max(value = 12, message = "must be at most 12")
        Integer level
) {
}
