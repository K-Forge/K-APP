package co.edu.konradlorenz.kapp.map.web;

import co.edu.konradlorenz.kapp.map.service.BuildingService;
import co.edu.konradlorenz.kapp.map.web.dto.CampusSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The campus (Sede) list, which clients call first to populate the campus selector.
 *
 * <p>Derived from the buildings rather than stored: a campus exists precisely when a
 * building is on it, so there is no second collection to keep in step.
 */
@RestController
@RequestMapping("/api/map/campuses")
@Tag(name = "Campuses", description = "The list of campuses (Sedes) known to the map.")
public class CampusController {

    private final BuildingService buildings;

    public CampusController(BuildingService buildings) {
        this.buildings = buildings;
    }

    @GetMapping
    @Operation(summary = "List campuses",
            description = "Every campus with at least one building, ordered by name. "
                    + "Allowed roles: ROLE_GUEST, ROLE_STUDENT, ROLE_PROFESSOR, ROLE_ADMIN.")
    public List<CampusSummaryResponse> list() {
        return buildings.listCampuses();
    }
}
