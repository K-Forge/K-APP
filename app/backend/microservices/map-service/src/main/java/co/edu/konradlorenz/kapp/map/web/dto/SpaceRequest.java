package co.edu.konradlorenz.kapp.map.web.dto;

import co.edu.konradlorenz.kapp.map.domain.SpaceType;
import co.edu.konradlorenz.kapp.map.domain.Wing;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "SpaceRequest")
public record SpaceRequest(
        @Schema(description = "Room code as printed on the door, including its wing where the "
                + "building uses them. Unique within the building identified by buildingCode.",
                example = "301-N")
        @NotBlank @Size(max = 20)
        String code,

        @Schema(description = "Which arm of the floor this space is in. Omit it and the server "
                + "derives it from a -N, -S or -C suffix on the code; send it explicitly when the "
                + "building names its wings some other way.",
                example = "NORTE")
        Wing wing,

        @Schema(example = "Laboratorio de Sistemas")
        @NotBlank @Size(max = 120)
        String name,

        @NotNull
        SpaceType type,

        @Schema(description = "Code of the building that contains this space. Must already exist.",
                example = "A")
        @NotBlank @Size(max = 10)
        String buildingCode,

        @Schema(description = "Level of the floor this space sits on. The building must already "
                + "have it. -1 is the basement.",
                example = "3")
        @NotNull @Min(-5) @Max(99)
        Integer floorLevel,

        @Schema(description = "Alternative names students actually search for.")
        List<@NotBlank @Size(max = 120) String> aliases,

        @Schema(description = "Top-left cell of the rectangle this space occupies, zero-based.",
                example = "4")
        @NotNull @Min(0)
        Integer gridRow,

        @Schema(example = "9")
        @NotNull @Min(0)
        Integer gridColumn,

        @Schema(description = "How many rows the space spans. Omit for an ordinary single-cell room.",
                example = "1")
        @Min(1) @Max(60)
        Integer rowSpan,

        @Schema(example = "1")
        @Min(1) @Max(60)
        Integer colSpan,

        @Schema(description = "Code of the lift, staircase or entrance that serves this space. "
                + "This is what produces \"piso 4, sube por el ascensor central\".",
                example = "ASC-CENTRAL")
        @Size(max = 20)
        String accessVia,

        @Schema(description = "Seating capacity, where it is known.", example = "25")
        @Min(0)
        Integer capacity
) {

    /** Never null, so callers do not have to keep checking. */
    public List<String> aliasesOrEmpty() {
        return aliases == null ? List.of() : aliases;
    }

    public int rowSpanOrOne() {
        return rowSpan == null ? 1 : rowSpan;
    }

    public int colSpanOrOne() {
        return colSpan == null ? 1 : colSpan;
    }
}
