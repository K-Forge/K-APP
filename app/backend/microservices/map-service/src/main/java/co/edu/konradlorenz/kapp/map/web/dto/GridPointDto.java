package co.edu.konradlorenz.kapp.map.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(name = "GridPoint", description = "One cell of a floor's grid. Zero-based, row first.")
public record GridPointDto(
        @NotNull @Min(0) Integer row,
        @NotNull @Min(0) Integer col
) {
}
