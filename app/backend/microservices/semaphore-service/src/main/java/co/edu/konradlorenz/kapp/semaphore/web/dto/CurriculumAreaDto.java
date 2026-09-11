package co.edu.konradlorenz.kapp.semaphore.web.dto;

import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumArea;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CurriculumAreaDto(
        // 20, not 10. The seeded plan uses slugs (CB, BIS, ISA, SI), but the area column of a
        // printed pensum holds words - COMPLEMENTARIA is fourteen characters - and the bulk
        // import accepted them happily while this rejected them, so a pensum imported through
        // the portal could not then be edited through the portal.
        @NotBlank @Size(max = 20) String code,
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
