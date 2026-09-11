package co.edu.konradlorenz.kapp.auth.client;

/**
 * Body of {@code POST /internal/users}, matching {@code docs/api/user.openapi.yaml}.
 *
 * <p>Auth-service owns credentials and nothing else, so this carries only what
 * user-service needs to materialise a profile. The e-mail is the natural key of the
 * upsert and <strong>must be lower-cased before it is sent</strong>: user-service
 * lower-cases on its side too, and if the two ever disagree a retry differing only in
 * capitalisation slips past the unique index and creates exactly the duplicate the upsert
 * exists to prevent.
 *
 * @param academic must be null for {@code ROLE_GUEST} and present for {@code ROLE_STUDENT};
 *                 user-service rejects a mismatch with 400
 */
public record InternalUserUpsert(
        String email,
        String firstName,
        String lastName,
        String role,
        AcademicInfo academic
) {

    /**
     * @param studentCode  university student code
     * @param programCode  academic program, {@code 506} is Ingenieria de Sistemas
     * @param pensumCode   pensum version; not carried by the registration contract, so
     *                     auth-service supplies a configured default
     * @param currentLevel semester, 1..12
     */
    public record AcademicInfo(
            String studentCode,
            String programCode,
            String pensumCode,
            Integer currentLevel
    ) {
    }
}
