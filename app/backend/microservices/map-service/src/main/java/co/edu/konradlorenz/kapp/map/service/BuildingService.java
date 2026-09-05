package co.edu.konradlorenz.kapp.map.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.common.error.DuplicateResourceException;
import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.map.domain.BuildingDocument;
import co.edu.konradlorenz.kapp.map.domain.BuildingRepository;
import co.edu.konradlorenz.kapp.map.domain.Floor;
import co.edu.konradlorenz.kapp.map.domain.SpaceDocument;
import co.edu.konradlorenz.kapp.map.domain.SpaceRepository;
import co.edu.konradlorenz.kapp.map.web.dto.BuildingRequest;
import co.edu.konradlorenz.kapp.map.web.dto.BuildingResponse;
import co.edu.konradlorenz.kapp.map.web.dto.CampusSummaryResponse;
import co.edu.konradlorenz.kapp.map.web.dto.FloorDetailResponse;
import co.edu.konradlorenz.kapp.map.web.dto.FloorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Buildings, their floors, and the campus list derived from them.
 *
 * <p>Buildings are addressed by {@code code} throughout, never by {@code id}: the code is
 * what is printed on the building and what a person types.
 */
@Service
public class BuildingService {

    private static final Logger log = LoggerFactory.getLogger(BuildingService.class);

    private final BuildingRepository buildings;
    private final SpaceRepository spaces;

    public BuildingService(BuildingRepository buildings, SpaceRepository spaces) {
        this.buildings = buildings;
        this.spaces = spaces;
    }

    public List<BuildingResponse> list(String campus) {
        List<BuildingDocument> found = StringUtils.hasText(campus)
                ? buildings.findAllByCampusOrderByCodeAsc(campus)
                : buildings.findAllByOrderByCodeAsc();
        return found.stream().map(MapMapper::toBuildingResponse).toList();
    }

    public BuildingResponse getByCode(String code) {
        return MapMapper.toBuildingResponse(require(code));
    }

    public BuildingResponse create(BuildingRequest request) {
        rejectDuplicateFloorLevels(request.floors());

        if (buildings.existsByCode(request.code())) {
            throw new DuplicateResourceException("Building", request.code());
        }

        Instant now = Instant.now();
        BuildingDocument saved = buildings.save(new BuildingDocument(
                UUID.randomUUID().toString(),
                request.code(),
                request.name(),
                request.campus(),
                request.description(),
                request.floors().stream().map(MapMapper::toFloor).toList(),
                false,
                now,
                now));

        log.info("Created building {} on campus {} with {} floors",
                saved.code(), saved.campus(), saved.floors().size());
        return MapMapper.toBuildingResponse(saved);
    }

    /**
     * Replaces a building, floors included.
     *
     * <p>Two rules earn their keep here. A floor may only disappear once it is empty,
     * because the alternative is orphaning every pin a colleague placed on it. And a
     * changed {@code code} or {@code campus} is pushed down onto the building's spaces,
     * which carry denormalised copies of both; skipping that is how a search result comes
     * back claiming a building that no longer exists.
     */
    public BuildingResponse update(String code, BuildingRequest request) {
        rejectDuplicateFloorLevels(request.floors());

        BuildingDocument existing = require(code);

        if (!existing.code().equals(request.code()) && buildings.existsByCode(request.code())) {
            throw new DuplicateResourceException("Building", request.code());
        }

        List<Floor> floors = request.floors().stream().map(MapMapper::toFloor).toList();
        rejectRemovingOccupiedFloors(existing, floors);

        BuildingDocument saved = buildings.save(new BuildingDocument(
                existing.id(),
                request.code(),
                request.name(),
                request.campus(),
                request.description(),
                floors,
                existing.placeholder(),
                existing.createdAt(),
                Instant.now()));

        propagateToSpaces(existing, saved);
        return MapMapper.toBuildingResponse(saved);
    }

    /**
     * Deletes an EMPTY building. A building that still holds spaces is refused with 409
     * rather than cascading: a cascade would take several hundred hand-placed pins with it
     * on behalf of someone who typed the wrong code.
     */
    public void delete(String code) {
        BuildingDocument existing = require(code);

        if (spaces.existsByBuildingId(existing.id())) {
            throw new MapConflictException(
                    "Building %s still has spaces and cannot be deleted. Delete its spaces first."
                            .formatted(existing.code()));
        }

        buildings.delete(existing);
        log.info("Deleted building {}", existing.code());
    }

    /**
     * One floor and every space on it - the call that renders a floor plan screen.
     */
    public FloorDetailResponse getFloor(String code, int level) {
        BuildingDocument building = require(code);
        Floor floor = building.floorAt(level).orElseThrow(() -> new ResourceNotFoundException(
                "Floor %d of building %s".formatted(level, code)));

        List<SpaceDocument> onFloor =
                spaces.findByBuildingIdAndFloorLevelOrderByCodeAsc(building.id(), level);

        return MapMapper.toFloorDetail(building, floor, onFloor);
    }

    /**
     * Every campus that has at least one building, with its building count.
     *
     * <p>Grouped in memory rather than with an aggregation on purpose: a university has a
     * handful of campuses and a few dozen buildings, so the pipeline would cost more to read
     * than it saves to run.
     */
    public List<CampusSummaryResponse> listCampuses() {
        Map<String, Long> byCampus = buildings.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.groupingBy(BuildingDocument::campus,
                        LinkedHashMap::new, Collectors.counting()));

        return byCampus.entrySet().stream()
                .map(entry -> new CampusSummaryResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CampusSummaryResponse::name))
                .toList();
    }

    /** Shared by the space service, which needs the building a space is being put into. */
    BuildingDocument require(String code) {
        return buildings.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Building", code));
    }

    private void rejectDuplicateFloorLevels(List<FloorDto> floors) {
        Set<Integer> seen = new java.util.HashSet<>();
        List<ApiError.FieldIssue> duplicates = floors.stream()
                .filter(floor -> !seen.add(floor.level()))
                .map(floor -> new ApiError.FieldIssue("floors",
                        "Duplicate floor level: " + floor.level()))
                .toList();

        if (!duplicates.isEmpty()) {
            throw new BusinessRuleException("A building cannot have two floors at the same level",
                    duplicates);
        }
    }

    private void rejectRemovingOccupiedFloors(BuildingDocument existing, List<Floor> replacement) {
        Set<Integer> keptLevels = replacement.stream().map(Floor::level).collect(Collectors.toSet());

        List<ApiError.FieldIssue> occupied = existing.floors().stream()
                .map(Floor::level)
                .filter(level -> !keptLevels.contains(level))
                .filter(level -> spaces.existsByBuildingIdAndFloorLevel(existing.id(), level))
                .map(level -> new ApiError.FieldIssue("floors",
                        "Floor %d still has spaces on it".formatted(level)))
                .toList();

        if (!occupied.isEmpty()) {
            throw new MapConflictException(
                    "Building %s cannot drop a floor that still has spaces on it"
                            .formatted(existing.code()),
                    occupied);
        }
    }

    /**
     * Spaces carry the building's {@code code} and {@code campus} so that a search result
     * needs no join. That copy has to be kept honest whenever the original changes.
     */
    private void propagateToSpaces(BuildingDocument before, BuildingDocument after) {
        boolean codeChanged = !before.code().equals(after.code());
        boolean campusChanged = !before.campus().equals(after.campus());

        if (!codeChanged && !campusChanged) {
            return;
        }

        List<SpaceDocument> affected = spaces.findByBuildingId(after.id()).stream()
                .map(space -> new SpaceDocument(
                        space.id(),
                        space.code(),
                        space.baseCode(),
                        space.wing(),
                        space.name(),
                        space.type(),
                        space.buildingId(),
                        after.code(),
                        after.campus(),
                        space.floorLevel(),
                        space.aliases(),
                        space.gridRow(),
                        space.gridColumn(),
                        space.rowSpan(),
                        space.colSpan(),
                        space.accessVia(),
                        space.capacity(),
                        space.placeholder(),
                        space.createdAt(),
                        Instant.now()))
                .toList();

        if (!affected.isEmpty()) {
            spaces.saveAll(affected);
            log.info("Propagated building {} rename/campus change to {} spaces",
                    after.code(), affected.size());
        }
    }
}
