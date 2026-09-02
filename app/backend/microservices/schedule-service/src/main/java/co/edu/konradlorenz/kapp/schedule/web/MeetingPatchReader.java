package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.schedule.web.dto.MeetingPeriodRequest;
import co.edu.konradlorenz.kapp.schedule.web.dto.Patched;
import co.edu.konradlorenz.kapp.schedule.web.dto.UpdateMeetingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns the raw JSON of {@code PATCH .../meetings/{meetingId}} into a validated
 * {@link UpdateMeetingRequest}, the same way {@link EnrollmentPatchReader} does for
 * enrollments.
 */
@Component
public class MeetingPatchReader {

    private static final Set<String> EDITABLE = Set.of("dayOfWeek", "startTime", "endTime", "periods");

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public MeetingPatchReader(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public UpdateMeetingRequest read(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new BusinessRuleException("The request body must be a JSON object");
        }
        if (body.isEmpty()) {
            throw new BusinessRuleException("Send at least one field to update");
        }

        List<ApiError.FieldIssue> issues = new ArrayList<>();
        rejectUnknownFields(body, issues);

        UpdateMeetingRequest patch = new UpdateMeetingRequest(
                dayOfWeek(body, issues),
                startTime(body, issues),
                endTime(body, issues),
                periods(body, issues));

        if (!issues.isEmpty()) {
            throw new BusinessRuleException("Validation failed", issues);
        }
        return patch;
    }

    private void rejectUnknownFields(JsonNode body, List<ApiError.FieldIssue> issues) {
        Set<String> names = new LinkedHashSet<>();
        body.fieldNames().forEachRemaining(names::add);
        for (String name : names) {
            if (!EDITABLE.contains(name)) {
                issues.add(new ApiError.FieldIssue(name, "is not a field of this resource"));
            }
        }
    }

    private Patched<DayOfWeek> dayOfWeek(JsonNode body, List<ApiError.FieldIssue> issues) {
        JsonNode node = body.get("dayOfWeek");
        if (node == null) {
            return Patched.absent();
        }
        if (node.isNull() || !node.isTextual()) {
            issues.add(new ApiError.FieldIssue("dayOfWeek", "must not be null"));
            return Patched.absent();
        }
        try {
            return Patched.of(DayOfWeek.valueOf(node.textValue()));
        } catch (IllegalArgumentException e) {
            issues.add(new ApiError.FieldIssue("dayOfWeek", "must be a valid day of week, for example MONDAY"));
            return Patched.absent();
        }
    }

    private Patched<LocalTime> startTime(JsonNode body, List<ApiError.FieldIssue> issues) {
        return time(body, "startTime", issues);
    }

    private Patched<LocalTime> endTime(JsonNode body, List<ApiError.FieldIssue> issues) {
        return time(body, "endTime", issues);
    }

    private Patched<LocalTime> time(JsonNode body, String field, List<ApiError.FieldIssue> issues) {
        JsonNode node = body.get(field);
        if (node == null) {
            return Patched.absent();
        }
        if (node.isNull() || !node.isTextual()) {
            issues.add(new ApiError.FieldIssue(field, "must not be null"));
            return Patched.absent();
        }
        try {
            return Patched.of(LocalTime.parse(node.textValue()));
        } catch (DateTimeParseException e) {
            issues.add(new ApiError.FieldIssue(field, "must be a time in HH:mm form, for example 18:15"));
            return Patched.absent();
        }
    }

    /**
     * Replaces the whole list when present - the contract is explicit that a delta would
     * make the disjoint date ranges ambiguous - so the {@link Patched} here always carries
     * the complete new list, never a partial one.
     */
    private Patched<List<MeetingPeriodRequest>> periods(JsonNode body, List<ApiError.FieldIssue> issues) {
        JsonNode node = body.get("periods");
        if (node == null) {
            return Patched.absent();
        }
        if (node.isNull() || !node.isArray() || node.isEmpty()) {
            issues.add(new ApiError.FieldIssue("periods", "must be a non-empty array"));
            return Patched.absent();
        }

        List<MeetingPeriodRequest> parsed = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            JsonNode element = node.get(i);
            MeetingPeriodRequest period;
            try {
                period = objectMapper.treeToValue(element, MeetingPeriodRequest.class);
            } catch (Exception e) {
                issues.add(new ApiError.FieldIssue("periods[" + i + "]", "is not a valid date range"));
                continue;
            }
            for (ConstraintViolation<MeetingPeriodRequest> violation : validator.validate(period)) {
                issues.add(new ApiError.FieldIssue(
                        "periods[" + i + "]." + violation.getPropertyPath(), violation.getMessage()));
            }
            parsed.add(period);
        }
        return Patched.of(parsed);
    }
}
