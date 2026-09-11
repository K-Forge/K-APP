package co.edu.konradlorenz.kapp.schedule.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

/**
 * Body of {@code POST /api/schedule/me/enrollments}.
 *
 * <p>Every course-describing field here is stored as a write-time snapshot, never as a
 * live reference back to {@code semaphore-service}'s catalogue - see {@code Enrollment}.
 * {@code subgroup} follows the same "explicit null, never omitted" rule as
 * {@code MeetingPeriodRequest.room}, for the same reason.
 */
public record CreateEnrollmentRequest(

        @NotBlank(message = "must not be blank")
        String courseCode,

        @NotBlank(message = "must not be blank")
        String pensumItemCode,

        @NotBlank(message = "must not be blank")
        String courseName,

        @NotNull(message = "must not be null")
        @Min(value = 1, message = "must be at least 1")
        @Max(value = 12, message = "must be at most 12")
        Integer level,

        @NotNull(message = "must not be null")
        @Min(value = 0, message = "must be zero or greater")
        Integer credits,

        @NotNull(message = "must not be null")
        @Min(value = 0, message = "must be zero or greater")
        Integer totalHours,

        @NotBlank(message = "must not be blank")
        String group,

        @JsonProperty(required = true)
        String subgroup,

        @NotBlank(message = "must not be blank")
        String professor,

        @NotBlank(message = "must not be blank")
        String campus,

        @NotNull(message = "must not be null")
        LocalDate startDate,

        @NotNull(message = "must not be null")
        LocalDate endDate,

        @NotBlank(message = "must not be blank")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a hex colour, for example #539392")
        String color,

        @NotNull(message = "must not be null")
        @Valid
        List<CreateMeetingRequest> meetings
) {
}
