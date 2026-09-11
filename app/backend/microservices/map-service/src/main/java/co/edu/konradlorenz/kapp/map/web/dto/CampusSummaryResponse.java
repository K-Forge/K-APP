package co.edu.konradlorenz.kapp.map.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The {@code CampusSummary} schema: a campus (Sede) and how many buildings the map holds
 * for it. Clients call this first, to populate the campus selector.
 */
@Schema(name = "CampusSummary")
public record CampusSummaryResponse(
        String name,
        long buildingCount
) {
}
