package co.edu.konradlorenz.kapp.schedule.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * One entry of {@code periods[]} in {@code CreateMeetingRequest} / {@code MeetingPeriod}.
 *
 * <p>{@code room} is {@code required(room, from, to)} in the contract even though it is
 * nullable: "no room assigned" must be sent as an explicit {@code null}, never by omitting
 * the key, so that it is never confused with "room unknown". {@code @JsonProperty(required
 * = true)} enforces the key's presence during deserialization while still accepting a
 * {@code null} value - {@code @NotNull} alone cannot do this, because it only ever sees the
 * value Jackson already produced for a missing key, which is the same {@code null} it
 * would produce for an explicit one.
 */
public record MeetingPeriodRequest(

        @NotNull(message = "must not be null")
        LocalDate from,

        @NotNull(message = "must not be null")
        LocalDate to,

        @JsonProperty(required = true)
        @Pattern(regexp = "^[0-9]{3,4}$", message = "must be a 3 or 4 digit room code, or null")
        String room
) {
}
