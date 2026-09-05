package co.edu.konradlorenz.kapp.map.web.dto;

import co.edu.konradlorenz.kapp.map.domain.SpaceType;
import co.edu.konradlorenz.kapp.map.domain.Wing;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "Space")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpaceResponse(
        String id,
        String code,
        String baseCode,
        Wing wing,
        String name,
        SpaceType type,
        String buildingId,
        String buildingCode,
        String campus,
        int floorLevel,
        List<String> aliases,
        int gridRow,
        int gridColumn,
        int rowSpan,
        int colSpan,
        String accessVia,
        Integer capacity
) {
}
