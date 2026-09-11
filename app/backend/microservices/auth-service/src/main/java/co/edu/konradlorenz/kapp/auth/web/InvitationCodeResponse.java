package co.edu.konradlorenz.kapp.auth.web;

import co.edu.konradlorenz.kapp.auth.domain.InvitationCode;

import java.time.Instant;

/**
 * An invitation code as the admin API returns it.
 *
 * <p>The internal {@code id} is not exposed: the code itself is the identifier every
 * other endpoint addresses it by, and publishing a second one only invites clients to
 * use the wrong one.
 */
public record InvitationCodeResponse(
        String code,
        String role,
        int maxUses,
        int timesUsed,
        boolean active,
        Instant expiresAt,
        String notes
) {

    public static InvitationCodeResponse from(InvitationCode c) {
        return new InvitationCodeResponse(c.code(), c.role(), c.maxUses(), c.timesUsed(),
                c.active(), c.expiresAt(), c.notes());
    }
}
