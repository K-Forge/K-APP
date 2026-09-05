package co.edu.konradlorenz.kapp.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A one-day pass reception hands to a visitor, and the record of who redeemed it.
 *
 * <p>Replaces open guest registration, which accepted any e-mail address, created a real
 * account, and left it behind forever. A visitor is on campus for an afternoon; an account
 * is a thing that outlives them. This is a token instead: no e-mail, no password, nothing
 * to clean up, and a redeemed pass opens the campus map and nothing else.
 *
 * <h2>This document holds personal data</h2>
 * {@code documentNumber} and {@code visitorName} are personal data under <strong>Ley 1581 de
 * 2012</strong> (habeas data). Before this existed, KApp stored no institutional or
 * government-issued data at all, and that change has obligations attached: a stated purpose
 * — reception knowing who was in the building — a retention period, and deletion when it
 * ends.
 *
 * <p>{@code purgeAt} is what enforces the retention. A TTL index on it makes MongoDB delete
 * the document itself, roughly a minute after the instant passes. That is deliberately not a
 * scheduled job in this service: a job that stops running leaves the data sitting there,
 * and nobody notices until somebody asks.
 *
 * @param code             what reception reads out to the visitor
 * @param issuedBy         the administrator who minted it, so a pass traces back to a person
 * @param redeemableUntil  after this the code is dead whether or not it was used. A pass is
 *                         for today
 * @param redeemedAt       null until somebody presents a document. Non-null means this row
 *                         is now a record of a visit, not an unused pass
 * @param accessExpiresAt  when the issued token stops working, 24 hours after redemption
 * @param purgeAt          when this whole document disappears
 */
@Document(collection = "visitor_passes")
public record VisitorPass(
        @Id String id,
        String code,
        String issuedBy,
        String notes,
        Instant createdAt,
        Instant redeemableUntil,
        Instant redeemedAt,
        DocumentType documentType,
        String documentNumber,
        String visitorName,
        Instant accessExpiresAt,
        Instant purgeAt
) {

    public static final String COLLECTION = "visitor_passes";

    public boolean redeemed() {
        return redeemedAt != null;
    }

    public boolean expiredAt(Instant now) {
        return redeemableUntil != null && !now.isBefore(redeemableUntil);
    }
}
