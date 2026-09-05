package co.edu.konradlorenz.kapp.map.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Everything a client needs to draw one floor in a single call: the grid's dimensions, the
 * corridors that cross it, and every space on it.
 */
@Schema(name = "FloorDetail")
public record FloorDetailResponse(
        int level,
        String name,
        int gridRows,
        int gridColumns,
        List<CorridorDto> corridors,
        String buildingId,
        String buildingCode,
        String buildingName,
        String campus,
        List<SpaceResponse> spaces
) {
}
