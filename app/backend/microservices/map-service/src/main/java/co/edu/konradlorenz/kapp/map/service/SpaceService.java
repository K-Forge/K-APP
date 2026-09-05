package co.edu.konradlorenz.kapp.map.service;

import co.edu.konradlorenz.kapp.common.error.DuplicateResourceException;
import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.map.domain.BuildingDocument;
import co.edu.konradlorenz.kapp.map.domain.Floor;
import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.common.error.ConflictException;
import co.edu.konradlorenz.kapp.map.domain.SpaceDocument;
import co.edu.konradlorenz.kapp.map.domain.Wing;
import co.edu.konradlorenz.kapp.map.domain.SpaceRepository;
import co.edu.konradlorenz.kapp.map.domain.SpaceType;
import co.edu.konradlorenz.kapp.map.web.dto.PageResponse;
import co.edu.konradlorenz.kapp.map.web.dto.SpaceDetailResponse;
import co.edu.konradlorenz.kapp.map.web.dto.SpaceRequest;
import co.edu.konradlorenz.kapp.map.web.dto.SpaceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spaces: the search, and the room-code lookup the schedule screen depends on.
 *
 * <p>A space is always addressed by its room code, because that is the key
 * {@code schedule-service} stores against a class and the number printed on the door. The
 * code is unique within a building but not across the map, so every lookup here can come
 * back ambiguous and says so rather than guessing.
 */
@Service
public class SpaceService {

    private static final Logger log = LoggerFactory.getLogger(SpaceService.class);

    private final SpaceRepository spaces;
    private final BuildingService buildingService;
    private final SpaceSearch search;

    public SpaceService(SpaceRepository spaces, BuildingService buildingService, SpaceSearch search) {
        this.spaces = spaces;
        this.buildingService = buildingService;
        this.search = search;
    }

    public PageResponse<SpaceResponse> search(String q, String campus, SpaceType type,
                                              String buildingCode, Wing wing, int page, int size) {
        SpaceSearch.Result result = search.search(q, campus, type, buildingCode, wing, page, size);
        return PageResponse.of(
                result.content().stream().map(MapMapper::toSpaceResponse).toList(),
                page,
                size,
                result.totalElements());
    }

    /**
     * The endpoint a student reaches by tapping a class in their timetable: one round trip
     * from a room code to the space, its floor plan and its building.
     */
    public SpaceDetailResponse getDetail(String code, String buildingCode) {
        SpaceDocument space = resolve(code, buildingCode);
        BuildingDocument building = buildingService.require(space.buildingCode());

        Floor floor = building.floorAt(space.floorLevel())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Floor %d of building %s".formatted(space.floorLevel(), building.code())));

        return MapMapper.toSpaceDetail(space, floor, building);
    }

    public SpaceResponse create(SpaceRequest request) {
        BuildingDocument building = requireBuildingWithFloor(request);

        if (spaces.findByBuildingIdAndCode(building.id(), request.code()).isPresent()) {
            throw new DuplicateResourceException(
                    "Space %s in building %s".formatted(request.code(), building.code()),
                    request.code());
        }
        checkFitsTheFloor(building, request, null);

        Instant now = Instant.now();
        SpaceDocument saved = spaces.save(new SpaceDocument(
                UUID.randomUUID().toString(),
                request.code(),
                SpaceDocument.baseCodeOf(request.code()),
                wingOf(request),
                request.name(),
                request.type(),
                building.id(),
                building.code(),
                building.campus(),
                request.floorLevel(),
                request.aliasesOrEmpty(),
                request.gridRow(),
                request.gridColumn(),
                request.rowSpanOrOne(),
                request.colSpanOrOne(),
                request.accessVia(),
                request.capacity(),
                false,
                now,
                now));

        log.info("Created space {} in building {} on floor {}",
                saved.code(), saved.buildingCode(), saved.floorLevel());
        return MapMapper.toSpaceResponse(saved);
    }

    /**
     * Replaces a space. Moving a room happens here, by sending a new grid cell - which is
     * exactly what the grid editor's export produces.
     */
    public SpaceResponse update(String code, String buildingCode, SpaceRequest request) {
        SpaceDocument current = resolve(code, buildingCode);
        BuildingDocument target = requireBuildingWithFloor(request);

        spaces.findByBuildingIdAndCode(target.id(), request.code())
                .filter(clash -> !clash.id().equals(current.id()))
                .ifPresent(clash -> {
                    throw new DuplicateResourceException(
                            "Space %s in building %s".formatted(request.code(), target.code()),
                            request.code());
                });
        checkFitsTheFloor(target, request, current.id());

        SpaceDocument saved = spaces.save(new SpaceDocument(
                current.id(),
                request.code(),
                SpaceDocument.baseCodeOf(request.code()),
                wingOf(request),
                request.name(),
                request.type(),
                target.id(),
                target.code(),
                target.campus(),
                request.floorLevel(),
                request.aliasesOrEmpty(),
                request.gridRow(),
                request.gridColumn(),
                request.rowSpanOrOne(),
                request.colSpanOrOne(),
                request.accessVia(),
                request.capacity(),
                // Provenance, not content: a corrected cell on an invented floor is still on
                // an invented floor. The flag clears when a real survey replaces the seed.
                current.placeholder(),
                current.createdAt(),
                Instant.now()));

        return MapMapper.toSpaceResponse(saved);
    }

    public void delete(String code, String buildingCode) {
        SpaceDocument target = resolve(code, buildingCode);
        spaces.delete(target);
        log.info("Deleted space {} from building {}", target.code(), target.buildingCode());
    }

    /**
     * Resolves a room code to exactly one space.
     *
     * <p>Three outcomes, and all three are in the contract: nothing matches (404), one
     * matches (the answer), or several buildings use the code and no disambiguator was
     * supplied (409, candidates named). The third is why {@code buildingCode} exists as a
     * query parameter instead of being baked into the path.
     */
    private SpaceDocument resolve(String code, String buildingCode) {
        List<SpaceDocument> matches = spaces.findByCodeOrderByBuildingCodeAsc(code);

        if (StringUtils.hasText(buildingCode)) {
            matches = matches.stream()
                    .filter(space -> space.buildingCode().equals(buildingCode))
                    .toList();
        }

        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("Space", code);
        }
        if (matches.size() > 1) {
            throw new AmbiguousSpaceCodeException(code,
                    matches.stream().map(SpaceDocument::buildingCode).toList());
        }
        return matches.get(0);
    }

    private BuildingDocument requireBuildingWithFloor(SpaceRequest request) {
        BuildingDocument building = buildingService.require(request.buildingCode());

        if (!building.hasFloor(request.floorLevel())) {
            throw new ResourceNotFoundException("Floor %d of building %s"
                    .formatted(request.floorLevel(), building.code()));
        }
        return building;
    }

    /**
     * A space has to fit on the floor it claims, and it may not sit on top of another.
     *
     * <p>Both are cheap to check and expensive to discover later: a room outside the grid
     * simply does not render, and two rooms in the same cell render one on top of the other,
     * so the second one is invisible rather than obviously wrong. Whoever is capturing a
     * floor finds out immediately instead of when a student cannot find a classroom.
     *
     * @param excludeId the space being replaced, so an update does not collide with itself
     */
    private void checkFitsTheFloor(BuildingDocument building, SpaceRequest request,
                                    String excludeId) {
        Floor floor = building.floorAt(request.floorLevel())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Floor %d of building %s".formatted(request.floorLevel(), building.code())));

        int lastRow = request.gridRow() + request.rowSpanOrOne() - 1;
        int lastColumn = request.gridColumn() + request.colSpanOrOne() - 1;

        if (lastRow >= floor.gridRows() || lastColumn >= floor.gridColumns()) {
            throw new BusinessRuleException(
                    "Space %s does not fit on floor %d of building %s, which is %d x %d"
                            .formatted(request.code(), floor.level(), building.code(),
                                    floor.gridRows(), floor.gridColumns()),
                    List.of(new ApiError.FieldIssue("gridRow",
                            "the space would occupy rows %d-%d and columns %d-%d"
                                    .formatted(request.gridRow(), lastRow,
                                            request.gridColumn(), lastColumn))));
        }

        SpaceDocument candidate = new SpaceDocument(
                null, request.code(), null, null, request.name(), request.type(),
                building.id(), building.code(), building.campus(), request.floorLevel(),
                List.of(), request.gridRow(), request.gridColumn(),
                request.rowSpanOrOne(), request.colSpanOrOne(), null, null, false, null, null);

        spaces.findByBuildingIdAndFloorLevelOrderByCodeAsc(building.id(), request.floorLevel()).stream()
                .filter(other -> excludeId == null || !other.id().equals(excludeId))
                .filter(candidate::overlaps)
                .findFirst()
                .ifPresent(other -> {
                    throw new ConflictException(
                            "Space %s would overlap %s on floor %d of building %s"
                                    .formatted(request.code(), other.code(),
                                            request.floorLevel(), building.code()),
                            List.of(new ApiError.FieldIssue("gridRow", other.code())));
                });
    }

    /**
     * The wing the caller sent, or the one implied by a {@code -N} / {@code -S} / {@code -C}
     * suffix on the code.
     *
     * <p>Derived only as a fallback. Deriving it always would be wrong the first time a
     * building names its wings something else, and refusing to derive it at all would mean
     * every one of the central building's rooms has to repeat what its own code already
     * says.
     */
    private static Wing wingOf(SpaceRequest request) {
        return request.wing() != null ? request.wing() : SpaceDocument.wingOf(request.code());
    }
}
