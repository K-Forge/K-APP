package co.edu.konradlorenz.kapp.user.domain;

/**
 * The single role an account holds.
 *
 * <p>The constants carry the {@code ROLE_} prefix in their own names on purpose: the JWT
 * {@code roles} claim, the OpenAPI enum and this type then all spell the value the same
 * way, so Jackson needs no custom naming and a profile round-trips through the contract
 * unchanged. See {@code KappRoles} in {@code common} for the matching authority names.
 */
public enum UserRole {
    ROLE_GUEST,
    ROLE_STUDENT,
    ROLE_PROFESSOR,
    ROLE_ADMIN;

    /** @return true for the one role that has no academic record by definition */
    public boolean isGuest() {
        return this == ROLE_GUEST;
    }
}
