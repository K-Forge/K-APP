package co.edu.konradlorenz.kapp.semaphore.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The slice of {@code user-service}'s {@code UserProfile} this service actually reads.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} is load-bearing, not decorative:
 * the real payload also carries {@code email}, {@code firstName}, {@code identification}
 * and more, none of which this service has any business holding onto. Without it, the
 * two contracts would be accidentally coupled - a field user-service adds tomorrow would
 * break deserialisation here today.
 *
 * @param id       the profile id, equal to the JWT {@code sub}
 * @param role     the account's role, e.g. {@code "ROLE_STUDENT"}
 * @param academic the academic record, or {@code null} for a {@code ROLE_GUEST} account
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserProfileResponse(
        String id,
        String role,
        Academic academic
) {

    /**
     * <p>{@code pensumCode} is deliberately not read here even though user-service's
     * payload carries one: this service is the source of truth for which pensum a
     * student is pinned to, resolved independently from {@code programCode} via the
     * catalog's own {@code ACTIVE} curriculum. Trusting a second copy of that fact would
     * reintroduce exactly the drift {@link co.edu.konradlorenz.kapp.semaphore.domain.Program}
     * avoids by not storing {@code activePensumCode}.
     *
     * @param studentCode  institutional student code
     * @param programCode  the program the lazily-created semaforo is pinned from
     * @param currentLevel the level user-service has on file; seeds the semaforo's own
     *                     {@code currentLevel} on first creation only
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Academic(
            String studentCode,
            String programCode,
            Integer currentLevel
    ) {
    }
}
