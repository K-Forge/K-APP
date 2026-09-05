package co.edu.konradlorenz.kapp.auth.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * What an administrator supplies to mint a code.
 *
 * <p>{@code timesUsed} and {@code active} are absent: a new code has always been used
 * zero times and is always active, and accepting either would let a caller mint a code
 * that is already spent or already off.
 *
 * @param code    upper case, digits and hyphens, so it survives being read aloud over a
 *                counter or a phone
 * @param role    the role a registration through this code receives. {@code ROLE_ADMIN}
 *                is rejected - see {@code InvitationCodeService#create}
 * @param maxUses capped at 10000; a code with no practical ceiling is an open door with
 *                extra steps
 */
public record InvitationCodeRequest(
        @NotBlank @Size(min = 6, max = 40) @Pattern(regexp = "^[A-Z0-9-]+$") String code,
        @NotBlank String role,
        @NotNull @Min(1) @Max(10000) Integer maxUses,
        Instant expiresAt,
        @Size(max = 200) String notes
) {
}
