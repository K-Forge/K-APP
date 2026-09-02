package co.edu.konradlorenz.kapp.schedule.web.dto;

import java.time.LocalDate;

/** Wire shape of {@code MeetingPeriod}. Built only by {@code mapper.ScheduleMapper}. */
public record MeetingPeriodResponse(LocalDate from, LocalDate to, String room) {
}
