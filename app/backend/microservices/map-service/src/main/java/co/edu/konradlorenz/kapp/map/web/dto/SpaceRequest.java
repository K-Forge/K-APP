package co.edu.konradlorenz.kapp.map.web.dto;

import co.edu.konradlorenz.kapp.map.domain.SpaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for creating or replacing a space.
 *
 * <p>{@code id}, {@code buildingId} and {@code campus} are derived server-side from
 * {@code buildingCode} and are never accepted from the client: a client that could set
 * {@code campus} independently could put a space on a campus its building is not on.
 *
 * <p>{@code x} and {@code y} are percentages of the plan image, not pixels. They are boxed
 * so that a missing coordinate fails validation instead of silently defaulting to 0, which
 * would park the pin in the top-left corner of the plan.
 */
@Schema(name = "SpaceRequest")
public record SpaceRequest(

        @Schema(description = "Room code, unique within the building identified by buildingCode.", example = "302")
        @NotBlank @Size(max = 20)
        String code,

        @Schema(example = "Laboratorio de Sistemas")
        @NotBlank @Size(max = 120)
        String name,

        @NotNull
        SpaceType type,

        @Schema(description = "Code of the building that contains this space. Must already exist.", example = "A")
        @NotBlank @Size(max = 10)
        String buildingCode,

        @Schema(description = "Level of the floor this space sits on. The building must already have it.", example = "3")
        @NotNull @Min(0) @Max(99)
        Integer floorLevel,

        @Schema(description = "Alternative names students actually search for.")
        List<@NotBlank @Size(max = 120) String> aliases,

        @Schema(description = "Pin position as a percentage of the plan image's width.", example = "27.4")
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0")
        Double x,

        @Schema(description = "Pin position as a percentage of the plan image's height.", example = "55.8")
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0")
        Double y,

        @Schema(description = "Seating capacity, where it is known.", example = "25")
        @Min(0)
        Integer capacity
) {

    /** Never null, so callers do not have to keep checking. */
    public List<String> aliasesOrEmpty() {
        return aliases == null ? List.of() : aliases;
    }
}
