package co.edu.konradlorenz.kapp.auth.client;

/**
 * The slice of {@code UserProfile} that registration actually uses.
 *
 * <p>Only {@code id} and {@code email} are read: the id becomes the {@code sub} claim of
 * every token signed for this account. The rest of the profile is user-service's business
 * and is deliberately not modelled here, so a field added there is not a change here.
 */
public record UserProfileView(String id, String email) {
}
