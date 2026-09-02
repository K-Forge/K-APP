package co.edu.konradlorenz.kapp.map.service;

import co.edu.konradlorenz.kapp.common.error.DuplicateResourceException;
import co.edu.konradlorenz.kapp.common.error.ResourceNotFoundException;
import co.edu.konradlorenz.kapp.map.domain.BuildingDocument;
import co.edu.konradlorenz.kapp.map.domain.Floor;
import co.edu.konradlorenz.kapp.map.domain.SpaceDocument;
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
                                              String buildingCode, int page, int size) {
        SpaceSearch.Result result = search.search(q, campus, type, buildingCode, page, size);
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

        Instant now = Instant.now();
        SpaceDocument saved = spaces.save(new SpaceDocument(
                UUID.randomUUID().toString(),
                request.code(),
                request.name(),
                request.type(),
                building.id(),
                building.code(),
                building.campus(),
                request.floorLevel(),
                request.aliasesOrEmpty(),
                request.x(),
                request.y(),
                request.capacity(),
                false,
                now,
                now));

        log.info("Created space {} in building {} on floor {}",
                saved.code(), saved.buildingCode(), saved.floorLevel());
        return MapMapper.toSpaceResponse(saved);
    }

    /**
     * Replaces a space. Moving a pin happens here, by sending new {@code x}/{@code y}
     * percentages - which is exactly what the pin editor's export produces.
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

        SpaceDocument saved = spaces.save(new SpaceDocument(
                current.id(),
                request.code(),
                request.name(),
                request.type(),
                target.id(),
                target.code(),
                target.campus(),
                request.floorLevel(),
                request.aliasesOrEmpty(),
                request.x(),
                request.y(),
                request.capacity(),
                // Provenance, not content: a corrected pin on an invented plan is still on
                // an invented plan. The flag clears when the real plans replace the seed.
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
}
