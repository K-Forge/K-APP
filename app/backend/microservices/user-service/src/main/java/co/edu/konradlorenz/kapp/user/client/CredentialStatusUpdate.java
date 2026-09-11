package co.edu.konradlorenz.kapp.user.client;

/** Body of {@code PATCH /internal/credentials/{userId}/status} in auth-service. */
public record CredentialStatusUpdate(boolean active) {
}
