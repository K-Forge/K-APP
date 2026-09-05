package co.edu.konradlorenz.kapp.auth.web;

import jakarta.validation.constraints.NotNull;

/**
 * The body of {@code PATCH /auth/admin/invitation-codes/{code}}.
 *
 * <p>Only {@code active} is changeable after minting. Raising {@code maxUses} on a live
 * code, or moving its expiry, would quietly widen access that was already granted and
 * audited at a particular size; minting a second code is the honest way to do that.
 */
public record InvitationCodeStatusRequest(@NotNull Boolean active) {
}
