package co.edu.konradlorenz.kapp.map.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "Floor",
        description = "One floor of a building, described as a grid the client draws rather than "
                + "as a plan image it overlays.")
public record FloorDto(
        @Schema(description = "Floor number. Matches the first digit of the room codes on this "
                + "floor, except in the basement, which is -1 and whose rooms are coded however "
                + "the building codes them.",
                example = "7")
        @NotNull @Min(-5) @Max(99)
        Integer level,

        @Schema(description = "Display name of the floor, in Spanish as shown to students.",
                example = "Piso 7")
        @NotBlank @Size(max = 60)
        String name,

        @Schema(description = "Rows in this floor's grid.", example = "12")
        @NotNull @Min(1) @Max(60)
        Integer gridRows,

        @Schema(description = "Columns in this floor's grid.", example = "16")
        @NotNull @Min(1) @Max(60)
        Integer gridColumns,

        @Schema(description = "Walkable routes across this floor.")
        @Valid List<CorridorDto> corridors
) {

    /** Never null, so callers do not have to keep checking. */
    public List<CorridorDto> corridorsOrEmpty() {
        return corridors == null ? List.of() : corridors;
    }
}
