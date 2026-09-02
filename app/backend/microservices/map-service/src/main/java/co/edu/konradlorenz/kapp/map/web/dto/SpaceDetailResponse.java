package co.edu.konradlorenz.kapp.map.web.dto;

import co.edu.konradlorenz.kapp.map.domain.SpaceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The {@code SpaceDetail} schema: a space, the floor it sits on and a summary of its
 * building - everything the schedule screen needs to render a plan with a pin in ONE round
 * trip when a student taps a class.
 *
 * <p>The contract composes this with {@code allOf}, so the space's own fields are flat on
 * the object rather than nested under a key. They are therefore repeated here in the
 * contract's order rather than delegated to {@link SpaceResponse}: {@code @JsonUnwrapped}
 * would produce the same JSON but silently stops working the day this record needs to be
 * deserialised.
 */
@Schema(name = "SpaceDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpaceDetailResponse(
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
        Integer capacity,
        FloorDto floor,
        BuildingSummaryResponse building
) {
}
