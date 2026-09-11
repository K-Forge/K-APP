package co.edu.konradlorenz.kapp.semaphore.web.dto;

import co.edu.konradlorenz.kapp.semaphore.domain.Placement;

public record PlacementDto(String code, int plannedLevel) {

    public static PlacementDto from(Placement p) {
        return new PlacementDto(p.code(), p.plannedLevel());
    }
}
