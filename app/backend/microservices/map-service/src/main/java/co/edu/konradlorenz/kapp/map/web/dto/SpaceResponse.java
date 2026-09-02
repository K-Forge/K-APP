package co.edu.konradlorenz.kapp.map.web.dto;

import co.edu.konradlorenz.kapp.map.domain.SpaceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The {@code Space} schema. Denormalised on purpose: {@code buildingCode}, {@code campus}
 * and {@code floorLevel} ride along on the space so a search result can be rendered
 * without a second call.
 *
 * <p>{@code aliases} is always present, even when empty. {@code capacity} is omitted when
 * unknown, which is not the same as zero.
 */
@Schema(name = "Space")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpaceResponse(
        String id,
        String code,
        String name,
        SpaceType type,
        String buildingId,
        String buildingCode,
        String campus,
        int floorLevel,
        List<String> aliases,
        double x,
        double y,
        Integer capacity
) {
}
