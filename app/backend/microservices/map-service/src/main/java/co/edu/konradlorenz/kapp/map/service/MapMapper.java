package co.edu.konradlorenz.kapp.map.service;

import co.edu.konradlorenz.kapp.map.domain.BuildingDocument;
import co.edu.konradlorenz.kapp.map.domain.Corridor;
import co.edu.konradlorenz.kapp.map.domain.GridPoint;
import co.edu.konradlorenz.kapp.map.domain.Floor;
import co.edu.konradlorenz.kapp.map.domain.SpaceDocument;
import co.edu.konradlorenz.kapp.map.web.dto.BuildingResponse;
import co.edu.konradlorenz.kapp.map.web.dto.BuildingSummaryResponse;
import co.edu.konradlorenz.kapp.map.web.dto.CorridorDto;
import co.edu.konradlorenz.kapp.map.web.dto.FloorDetailResponse;
import co.edu.konradlorenz.kapp.map.web.dto.FloorDto;
import co.edu.konradlorenz.kapp.map.web.dto.GridPointDto;
import co.edu.konradlorenz.kapp.map.web.dto.SpaceDetailResponse;
import co.edu.konradlorenz.kapp.map.web.dto.SpaceResponse;

import java.util.List;

/**
 * Turns stored documents into the exact shapes {@code docs/api/map.openapi.yaml}
 * publishes.
 *
 * <p>The translation exists for one reason worth stating: documents carry a
 * {@code placeholder} flag that marks the invented seed campus, and that flag must never
 * reach a client. Returning documents straight from the controllers would publish it, and
 * would also make any future storage field an accidental addition to the API.
 */
public final class MapMapper {

    public static FloorDto toFloorDto(Floor floor) {
        return new FloorDto(floor.level(), floor.name(), floor.gridRows(), floor.gridColumns(),
                floor.corridors().stream().map(MapMapper::toCorridorDto).toList());
    }

    public static Floor toFloor(FloorDto dto) {
        return new Floor(dto.level(), dto.name(), dto.gridRows(), dto.gridColumns(),
                dto.corridorsOrEmpty().stream().map(MapMapper::toCorridor).toList());
    }

    public static CorridorDto toCorridorDto(Corridor corridor) {
        return new CorridorDto(corridor.code(), corridor.name(), corridor.color(),
                corridor.path().stream()
                        .map(point -> new GridPointDto(point.row(), point.col()))
                        .toList());
    }

    public static Corridor toCorridor(CorridorDto dto) {
        return new Corridor(dto.code(), dto.name(), dto.color(),
                dto.path().stream()
                        .map(point -> new GridPoint(point.row(), point.col()))
                        .toList());
    }

    public static BuildingResponse toBuildingResponse(BuildingDocument building) {
        return new BuildingResponse(
                building.id(),
                building.code(),
                building.name(),
                building.campus(),
                building.description(),
                building.floors().stream().map(MapMapper::toFloorDto).toList());
    }

    public static BuildingSummaryResponse toBuildingSummary(BuildingDocument building) {
        return new BuildingSummaryResponse(
                building.id(),
                building.code(),
                building.name(),
                building.campus(),
                building.description());
    }

    public static SpaceResponse toSpaceResponse(SpaceDocument space) {
        return new SpaceResponse(
                space.id(),
                space.code(),
                space.baseCode(),
                space.wing(),
                space.name(),
                space.type(),
                space.buildingId(),
                space.buildingCode(),
                space.campus(),
                space.floorLevel(),
                space.aliases(),
                space.gridRow(),
                space.gridColumn(),
                space.rowSpan(),
                space.colSpan(),
                space.accessVia(),
                space.capacity());
    }

    public static SpaceDetailResponse toSpaceDetail(SpaceDocument space,
                                                    Floor floor,
                                                    BuildingDocument building) {
        return new SpaceDetailResponse(
                space.id(),
                space.code(),
                space.baseCode(),
                space.wing(),
                space.name(),
                space.type(),
                space.buildingId(),
                space.buildingCode(),
                space.campus(),
                space.floorLevel(),
                space.aliases(),
                space.gridRow(),
                space.gridColumn(),
                space.rowSpan(),
                space.colSpan(),
                space.accessVia(),
                space.capacity(),
                toFloorDto(floor),
                toBuildingSummary(building));
    }

    public static FloorDetailResponse toFloorDetail(BuildingDocument building,
                                                    Floor floor,
                                                    List<SpaceDocument> spaces) {
        return new FloorDetailResponse(
                floor.level(),
                floor.name(),
                floor.gridRows(),
                floor.gridColumns(),
                floor.corridors().stream().map(MapMapper::toCorridorDto).toList(),
                building.id(),
                building.code(),
                building.name(),
                building.campus(),
                spaces.stream().map(MapMapper::toSpaceResponse).toList());
    }

    private MapMapper() {
    }
}
