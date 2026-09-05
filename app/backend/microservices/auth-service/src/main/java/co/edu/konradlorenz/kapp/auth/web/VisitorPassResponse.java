package co.edu.konradlorenz.kapp.auth.web;

import co.edu.konradlorenz.kapp.auth.domain.DocumentType;
import co.edu.konradlorenz.kapp.auth.domain.VisitorPass;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * A pass as reception sees it, including who redeemed it.
 *
 * <p>This is an administrative view and it deliberately carries the visitor's document: the
 * whole reason the record exists is that the desk can answer "who was here". It is returned
 * only to {@code ROLE_ADMIN}, and only from {@code /auth/admin/**}.
 */
@Schema(name = "VisitorPass")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisitorPassResponse(
        String code,
        String issuedBy,
        String notes,
        Instant createdAt,
        Instant redeemableUntil,
        boolean redeemed,
        Instant redeemedAt,
        DocumentType documentType,
        String documentNumber,
        String visitorName,
        Instant accessExpiresAt,
        Instant purgeAt
) {

    public static VisitorPassResponse from(VisitorPass pass) {
        return new VisitorPassResponse(
                pass.code(), pass.issuedBy(), pass.notes(), pass.createdAt(),
                pass.redeemableUntil(), pass.redeemed(), pass.redeemedAt(),
                pass.documentType(), pass.documentNumber(), pass.visitorName(),
                pass.accessExpiresAt(), pass.purgeAt());
    }
}
