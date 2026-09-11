package co.edu.konradlorenz.kapp.schedule.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.schedule.domain.Meeting;
import co.edu.konradlorenz.kapp.schedule.domain.MeetingOverlap;
import co.edu.konradlorenz.kapp.schedule.domain.MeetingPeriod;

import java.util.ArrayList;
import java.util.List;

/**
 * Structural checks a single {@link Meeting} must pass before it is even considered for
 * the cross-schedule overlap check in {@link MeetingConflictValidator}.
 *
 * <p>These are checks about one meeting's own internal consistency - not one field at a
 * time, which bean validation already covers on the request DTOs, but relationships
 * between fields that only exist once the meeting is assembled. Shared by both the create
 * and the update path, since both eventually produce a candidate {@link Meeting} that must
 * satisfy the same rules.
 */
final class MeetingWriteValidation {

    private MeetingWriteValidation() {
    }

    static void validate(Meeting meeting) {
        List<ApiError.FieldIssue> issues = new ArrayList<>();

        if (!meeting.startTime().isBefore(meeting.endTime())) {
            issues.add(new ApiError.FieldIssue("endTime", "must be later than startTime"));
        }

        List<MeetingPeriod> periods = meeting.periods();
        for (int i = 0; i < periods.size(); i++) {
            MeetingPeriod period = periods.get(i);

            if (period.to().isBefore(period.from())) {
                issues.add(new ApiError.FieldIssue("periods[" + i + "].to",
                        "must not be earlier than from"));
            }
            if (period.from().getDayOfWeek() != meeting.dayOfWeek()) {
                issues.add(new ApiError.FieldIssue("periods[" + i + "].from",
                        "must fall on " + meeting.dayOfWeek()));
            }
            if (period.to().getDayOfWeek() != meeting.dayOfWeek()) {
                issues.add(new ApiError.FieldIssue("periods[" + i + "].to",
                        "must fall on " + meeting.dayOfWeek()));
            }
            for (int j = i + 1; j < periods.size(); j++) {
                if (MeetingOverlap.periodsOverlap(period, periods.get(j))) {
                    issues.add(new ApiError.FieldIssue("periods[" + i + "]",
                            "overlaps periods[" + j + "]; a meeting's own date ranges must be disjoint"));
                }
            }
        }

        if (!issues.isEmpty()) {
            throw new BusinessRuleException("Validation failed", issues);
        }
    }
}
