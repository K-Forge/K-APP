package co.edu.konradlorenz.kapp.user.web.dto;

import co.edu.konradlorenz.kapp.user.domain.AcademicInfo;
import co.edu.konradlorenz.kapp.user.domain.Identification;

/**
 * A validated {@code PATCH /api/users/me} body.
 *
 * <p>Every field is a {@link Patched}, so the service can tell "not mentioned" from "set
 * to null" without inspecting JSON itself. Built by
 * {@code co.edu.konradlorenz.kapp.user.web.ProfilePatchReader}, which is the only place
 * that reads the raw tree.
 *
 * <p>{@code firstName} and {@code lastName} are not nullable in the contract, so they are
 * either absent or a real value here; the reader rejects an explicit null for them.
 */
public record ProfilePatch(
        Patched<String> firstName,
        Patched<String> lastName,
        Patched<String> phone,
        Patched<String> avatarUrl,
        Patched<Identification> identification,
        Patched<AcademicInfo> academic
) {
}
