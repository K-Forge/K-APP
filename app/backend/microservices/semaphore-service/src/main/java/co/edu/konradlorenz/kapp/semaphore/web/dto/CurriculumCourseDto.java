package co.edu.konradlorenz.kapp.semaphore.web.dto;

import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumCourse;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * One pensum item on the wire.
 *
 * <p>{@code totalHours} is nullable on write and always present on read: omit it and the
 * server derives {@code weeklyHours * 16}; supply it and it must satisfy that invariant
 * or the request is rejected. That is why it is validated in the service rather than here
 * - a bean-validation annotation cannot see a sibling field.
 *
 * <p>{@code sinuCode} is an addition to the published schema, not a change to it: an extra
 * nullable field that clients may ignore. It exists so a generated placeholder code is
 * never mistaken for an institutional one.
 */
public record CurriculumCourseDto(
        @Size(max = 20) String code,
        @NotBlank @Size(max = 30) String pensumItemCode,
        @NotBlank @Size(max = 120) String name,
        @Min(1) @Max(12) int level,
        @Min(0) int credits,
        @Min(0) int weeklyHours,
        @Min(0) Integer totalHours,
        @NotBlank @Size(max = 10) String area,
        @JsonProperty("isElectiveSlot") boolean isElectiveSlot,
        @NotNull List<@Size(max = 20) String> prerequisites,
        @Size(max = 20) String sinuCode
) {

    public static CurriculumCourseDto from(CurriculumCourse course) {
        return new CurriculumCourseDto(
                course.code(),
                course.pensumItemCode(),
                course.name(),
                course.level(),
                course.credits(),
                course.weeklyHours(),
                course.totalHours(),
                course.area(),
                course.electiveSlot(),
                course.prerequisites(),
                course.sinuCode());
    }

    public CurriculumCourse toDomain() {
        return new CurriculumCourse(code, pensumItemCode, name, level, credits, weeklyHours,
                area, isElectiveSlot, prerequisites, sinuCode);
    }
}
