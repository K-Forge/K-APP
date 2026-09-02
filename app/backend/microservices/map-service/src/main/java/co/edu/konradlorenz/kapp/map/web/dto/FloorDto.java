package co.edu.konradlorenz.kapp.map.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The {@code Floor} schema. Used on the way in and on the way out: the contract's request
 * and response shapes for a floor are identical, so one record carries both and the
 * validation constraints below are exactly the spec's.
 */
@Schema(name = "Floor", description = "One floor of a building and the static plan image that represents it.")
public record FloorDto(

        @Schema(description = "Floor number. Matches the first digit of the room codes on this floor.", example = "7")
        @NotNull @Min(0) @Max(99)
        Integer level,

        @Schema(description = "Display name of the floor, in Spanish as shown to students.", example = "Piso 7")
        @NotBlank @Size(max = 60)
        String name,

        @Schema(description = "Path to the floor plan image, served statically by the reverse proxy.",
                example = "/api/map/plans/bloque-a-p7.webp")
        @NotBlank @Size(max = 255)
        String planImageUrl,

        @Schema(description = "Intrinsic width of the plan image in pixels.", example = "2048")
        @NotNull @Min(1)
        Integer imageWidth,

        @Schema(description = "Intrinsic height of the plan image in pixels.", example = "1536")
        @NotNull @Min(1)
        Integer imageHeight
) {
}
