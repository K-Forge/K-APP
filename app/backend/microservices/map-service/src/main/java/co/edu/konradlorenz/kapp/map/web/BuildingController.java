package co.edu.konradlorenz.kapp.map.web;

import co.edu.konradlorenz.kapp.map.service.BuildingService;
import co.edu.konradlorenz.kapp.map.web.dto.BuildingRequest;
import co.edu.konradlorenz.kapp.map.web.dto.BuildingResponse;
import co.edu.konradlorenz.kapp.map.web.dto.FloorDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Buildings and their floors.
 *
 * <p>Access is not annotated here. It is decided in one place, by
 * {@code MapSecurityConfig}: every GET under {@code /api/map/**} is readable by
 * {@code ROLE_GUEST} and up, every write is {@code ROLE_ADMIN}. Scattering
 * {@code @PreAuthorize} across the controllers would make the guest rule - the one rule
 * unique to this service - something a reader has to reassemble from eleven methods.
 */
@RestController
@RequestMapping("/api/map/buildings")
@Validated
@Tag(name = "Buildings", description = "Campus buildings and the floors they contain.")
public class BuildingController {

    private final BuildingService buildings;

    public BuildingController(BuildingService buildings) {
        this.buildings = buildings;
    }

    @GetMapping
    @Operation(summary = "List buildings",
            description = "Every building with its floors, ordered by code. "
                    + "Allowed roles: ROLE_GUEST, ROLE_STUDENT, ROLE_PROFESSOR, ROLE_ADMIN.")
    public List<BuildingResponse> list(
            @RequestParam(required = false) @Size(min = 1, max = 120) String campus) {
        return buildings.list(campus);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get a building by code",
            description = "Allowed roles: ROLE_GUEST, ROLE_STUDENT, ROLE_PROFESSOR, ROLE_ADMIN.")
    public BuildingResponse get(@PathVariable @Size(min = 1, max = 10) String code) {
        return buildings.getByCode(code);
    }

    @PostMapping
    @Operation(summary = "Create a building", description = "Allowed roles: ROLE_ADMIN only.")
    public ResponseEntity<BuildingResponse> create(@Valid @RequestBody BuildingRequest request) {
        BuildingResponse created = buildings.create(request);
        return ResponseEntity
                .created(UriComponentsBuilder.fromPath("/api/map/buildings/{code}")
                        .buildAndExpand(created.code()).toUri())
                .body(created);
    }

    @PutMapping("/{code}")
    @Operation(summary = "Update a building",
            description = "Replaces the building including its floors. Dropping a floor that "
                    + "still has spaces on it is rejected with 409. Allowed roles: ROLE_ADMIN only.")
    public BuildingResponse update(@PathVariable @Size(min = 1, max = 10) String code,
                                   @Valid @RequestBody BuildingRequest request) {
        return buildings.update(code, request);
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete a building",
            description = "The building must be empty; a building that still has spaces is "
                    + "rejected with 409 and nothing is deleted. Allowed roles: ROLE_ADMIN only.")
    public ResponseEntity<Void> delete(@PathVariable @Size(min = 1, max = 10) String code) {
        buildings.delete(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}/floors/{level}")
    @Operation(summary = "Get a floor and every space on it",
            description = "The call that renders a floor plan screen: draw planImageUrl, then "
                    + "place one pin per space from its x/y percentages. "
                    + "Allowed roles: ROLE_GUEST, ROLE_STUDENT, ROLE_PROFESSOR, ROLE_ADMIN.")
    public FloorDetailResponse floor(@PathVariable @Size(min = 1, max = 10) String code,
                                     @PathVariable @Min(0) @Max(99) int level) {
        return buildings.getFloor(code, level);
    }
}
