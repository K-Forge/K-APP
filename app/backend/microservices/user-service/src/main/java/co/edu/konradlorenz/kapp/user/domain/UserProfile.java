package co.edu.konradlorenz.kapp.user.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * A KApp user profile. Holds no credential data whatsoever: no password, no hash, no
 * verification token. Those live in auth-service, and the two are joined by {@link #id},
 * which is the {@code sub} claim of every token signed for this account.
 *
 * <p>{@code academic} is null for every guest and populated for every member of the
 * university. Nothing may assume it is present.
 *
 * @param id           account identifier, shared with auth-service as the token subject
 * @param email        address the account authenticates with, always stored lowercased
 * @param searchTokens derived, never set by a caller - see the compact constructor
 * @param createdAt    account creation instant, at millisecond precision
 * @param updatedAt    last profile modification, at millisecond precision
 */
@Document(collection = "users")
public record UserProfile(

        @Id
        String id,

        String email,

        String firstName,

        String lastName,

        Identification identification,

        String phone,

        String avatarUrl,

        UserRole role,

        boolean active,

        AcademicInfo academic,

        @Field("searchTokens")
        List<String> searchTokens,

        Instant createdAt,

        Instant updatedAt
) {

    /**
     * Recomputes {@code searchTokens} from the fields it indexes, on every construction.
     *
     * <p>Spring Data builds this record through its canonical constructor when reading a
     * document back, so the derived field cannot drift from its sources even for data
     * written by an older version of the service, or by a hand-run script, or by a future
     * change unit. The alternative - recomputing at each write site - is one forgotten
     * call away from a profile that exists but can never be found.
     */
    public UserProfile {
        searchTokens = SearchTokens.forProfile(firstName, lastName, email);
    }

    /** @return a copy with the activation flag set, or this same instance when unchanged */
    public UserProfile withActive(boolean newActive, Instant now) {
        if (newActive == active) {
            return this;
        }
        return new UserProfile(id, email, firstName, lastName, identification, phone, avatarUrl,
                role, newActive, academic, searchTokens, createdAt, now);
    }
}
