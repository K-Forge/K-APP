package co.edu.konradlorenz.kapp.map.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The {@code FloorDetail} schema: one floor plus every space on it.
 *
 * <p>This is the call that renders a floor plan screen. The client draws
 * {@code planImageUrl} at whatever size the device allows and then places one pin per
 * space from its {@code x}/{@code y} percentages.
 */
@Schema(name = "FloorDetail")
public record FloorDetailResponse(
        int level,
        String name,
        String planImageUrl,
        int imageWidth,
        int imageHeight,
        String buildingId,
        String buildingCode,
        String buildingName,
        String campus,
        List<SpaceResponse> spaces
) {
}
