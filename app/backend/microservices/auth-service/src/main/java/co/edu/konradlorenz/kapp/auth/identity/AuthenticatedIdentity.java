package co.edu.konradlorenz.kapp.auth.identity;

import java.util.List;

/**
 * Who the caller turned out to be, once an {@link IdentityProviderPort} vouched for them.
 *
 * <p>Every field has an exact Entra ID equivalent, which is the point: an adapter for the
 * university's tenant fills this in from token claims rather than from a Mongo document,
 * and nothing downstream notices.
 *
 * <table>
 *   <caption>Mapping to Entra ID claims</caption>
 *   <tr><th>Field</th><th>Entra equivalent</th></tr>
 *   <tr><td>{@code subject}</td><td>{@code oid}</td></tr>
 *   <tr><td>{@code email}</td><td>{@code preferred_username}</td></tr>
 *   <tr><td>{@code roles}</td><td>app-role assignment</td></tr>
 *   <tr><td>{@code emailVerified}</td><td>implicit - the tenant owns the mailbox</td></tr>
 * </table>
 *
 * <p>Notice what is absent: no password hash, no Mongo id, no account status. Those are a
 * local storage concern, and a provider that keeps them out of this record is a provider
 * whose replacement is a genuine swap rather than a rewrite.
 *
 * @param subject       the stable user id that becomes the {@code sub} claim
 * @param email         the account e-mail
 * @param roles         prefixed role names, e.g. {@code ROLE_STUDENT}
 * @param emailVerified whether the address has actually been proven
 */
public record AuthenticatedIdentity(
        String subject,
        String email,
        List<String> roles,
        boolean emailVerified
) {
    public AuthenticatedIdentity {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
