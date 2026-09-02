package co.edu.konradlorenz.kapp.semaphore.web.dto;

import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumArea;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CurriculumAreaDto(
        @NotBlank @Size(max = 10) String code,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$",
                message = "must be an RGB hex triplet such as #539392") String color,
        @Min(0) int credits,
        @Min(0) int hours
) {

    public static CurriculumAreaDto from(CurriculumArea area) {
        return new CurriculumAreaDto(area.code(), area.name(), area.color(),
                area.credits(), area.hours());
    }

    public CurriculumArea toDomain() {
        return new CurriculumArea(code, name, color, credits, hours);
    }
}
