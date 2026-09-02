package co.edu.konradlorenz.kapp.user.web.dto;

import co.edu.konradlorenz.kapp.user.domain.AcademicInfo;
import co.edu.konradlorenz.kapp.user.domain.Identification;
import co.edu.konradlorenz.kapp.user.domain.UserProfile;
import co.edu.konradlorenz.kapp.user.domain.UserRole;

/**
 * The {@code UserProfile} schema of the contract, field for field and in the same order.
 *
 * <p>Deliberately not the document itself: {@code searchTokens}, {@code createdAt} and
 * {@code updatedAt} are storage concerns that the contract does not publish, and shipping
 * them would make the mobile clients depend on fields no one promised to keep.
 *
 * <p>{@code identification} and {@code academic} are serialised even when null, because
 * both are listed as required in the schema. A client is told the field exists and is
 * empty, rather than left to guess whether it was omitted or unset.
 */
public record UserProfileResponse(
        String id,
        String email,
        String firstName,
        String lastName,
        Identification identification,
        String phone,
        String avatarUrl,
        UserRole role,
        boolean active,
        AcademicInfo academic
) {

    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                profile.id(),
                profile.email(),
                profile.firstName(),
                profile.lastName(),
                profile.identification(),
                profile.phone(),
                profile.avatarUrl(),
                profile.role(),
                profile.active(),
                profile.academic());
    }
}
