package co.edu.konradlorenz.kapp.map.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;

import java.util.List;

/**
 * The room code addresses more than one space and no {@code buildingCode} was supplied.
 *
 * <p>Room codes are unique within a building, not across the map: Bloque A and Bloque B can
 * each have a 302. Picking one of them would send a student to the wrong side of campus, so
 * the request is refused with 409 and the candidate buildings are named in {@code details},
 * which is what the contract promises and what lets the client re-ask with the disambiguator.
 */
public class AmbiguousSpaceCodeException extends MapConflictException {

    public AmbiguousSpaceCodeException(String code, List<String> buildingCodes) {
        super(("Room code %s exists in more than one building. "
                        + "Pass buildingCode to choose one of: %s")
                        .formatted(code, String.join(", ", buildingCodes)),
                buildingCodes.stream()
                        .map(b -> new ApiError.FieldIssue("buildingCode", "Candidate building: " + b))
                        .toList());
    }
}
