package co.edu.konradlorenz.kapp.map.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for creating or replacing a building. {@code id} is server-generated and is
 * never accepted from the client, which is why it is absent here rather than ignored.
 *
 * <p>On {@code PUT} the {@code floors} list REPLACES the stored one, so a floor omitted
 * here is deleted - and deleting a floor that still has spaces on it fails with 409.
 */
@Schema(name = "BuildingRequest")
public record BuildingRequest(

        @Schema(description = "Building code, unique across the map.", example = "C")
        @NotBlank @Size(max = 10)
        String code,

        @Schema(example = "Bloque C")
        @NotBlank @Size(max = 120)
        String name,

        @Schema(description = "The campus (Sede) this building belongs to.", example = "Sede Principal")
        @NotBlank @Size(max = 120)
        String campus,

        @Schema(description = "Optional free-text description shown on the building screen.")
        @Size(max = 500)
        String description,

        @Schema(description = "The complete list of floors. At least one.")
        @NotNull @NotEmpty
        List<@NotNull @Valid FloorDto> floors
) {
}
