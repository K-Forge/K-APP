package co.edu.konradlorenz.kapp.map.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The {@code BuildingSummary} schema: a building WITHOUT its floors.
 *
 * <p>Returned inside {@code SpaceDetail}, where the one floor that matters is already
 * included in full and the other eleven would be dead weight on a mobile connection.
 */
@Schema(name = "BuildingSummary")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildingSummaryResponse(
        String id,
        String code,
        String name,
        String campus,
        String description
) {
}
