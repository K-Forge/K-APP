package co.edu.konradlorenz.kapp.auth.identity;

/**
 * The seam where Microsoft Entra ID plugs in. Not implemented, and deliberately so.
 *
 * <p>The university has confirmed there is no technical obstacle to OIDC on their side, so
 * this is a question of credentials rather than of design. What is missing is exactly four
 * things, all of which come from an application registration in the Konrad Lorenz tenant
 * that nobody has granted yet:
 *
 * <ol>
 *   <li><strong>Client id</strong> - the {@code appId} of the registration.</li>
 *   <li><strong>Client secret</strong> (or, better, a certificate) - to redeem the
 *       authorization code at the token endpoint. Never in the repository; it belongs in
 *       the environment beside {@code KAPP_JWT_PRIVATE_KEY}.</li>
 *   <li><strong>Tenant id</strong> - the directory the registration lives in. It decides
 *       which accounts can sign in at all, and pointing at {@code common} instead would
 *       let any Microsoft account in the world authenticate.</li>
 *   <li><strong>Discovery document</strong> -
 *       {@code https://login.microsoftonline.com/{tenant}/v2.0/.well-known/openid-configuration},
 *       which yields the authorization and token endpoints, the issuer, and the JWKS URI
 *       used to validate what Entra signs.</li>
 * </ol>
 *
 * <p>Filling those in is the whole job. The rest of the service already assumes nothing
 * about where an identity comes from: {@code AuthService} depends on
 * {@link IdentityProviderPort}, {@link AuthenticatedIdentity} carries only fields that have
 * an Entra equivalent, and {@link IdentityAssertion.AuthorizationCode} already exists as
 * the assertion this adapter would consume.
 *
 * <h2>What implementing it looks like</h2>
 * <ol>
 *   <li>Redeem {@code code} + {@code codeVerifier} at the token endpoint for an id token.</li>
 *   <li>Validate that token against Entra's JWKS - do not trust it unverified.</li>
 *   <li>Map claims onto {@link AuthenticatedIdentity}: {@code oid} to {@code subject},
 *       {@code preferred_username} to {@code email}, app-role assignments to
 *       {@code roles}, and {@code emailVerified} to true, since the tenant owns the
 *       mailbox.</li>
 *   <li>Reconcile with the local profile in user-service, keyed on e-mail, so an account
 *       that already exists is adopted rather than duplicated.</li>
 *   <li>Register it as a bean - add {@code @Component} - at which point
 *       {@code AuthService} starts routing authorization-code assertions here with no
 *       change of its own.</li>
 * </ol>
 *
 * <p><strong>This class is intentionally not a Spring bean.</strong> Nothing constructs it
 * and nothing calls it. Registering a provider that answers {@code true} to
 * {@link #supports} and then throws would turn a future authorization-code login into an
 * HTTP 500; leaving it unregistered means the assertion finds no provider at all, which
 * fails at the seam with a message naming what is missing.
 */
public class EntraIdAdapter implements IdentityProviderPort {

    public static final String PROVIDER_ID = "entra-id";

    private static final String NOT_CONFIGURED = """
            The Entra ID adapter is a documented seam, not an implementation. It needs a \
            client id, a client secret, a tenant id and the tenant's OIDC discovery \
            document, none of which exist until the university grants an application \
            registration. See the class javadoc for the five steps.""";

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean supports(IdentityAssertion assertion) {
        return assertion instanceof IdentityAssertion.AuthorizationCode;
    }

    @Override
    public AuthenticatedIdentity authenticate(IdentityAssertion assertion) {
        throw new UnsupportedOperationException(NOT_CONFIGURED);
    }
}
