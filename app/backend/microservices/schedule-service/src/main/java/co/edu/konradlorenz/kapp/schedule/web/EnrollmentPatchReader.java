package co.edu.konradlorenz.kapp.schedule.web;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.schedule.web.dto.Patched;
import co.edu.konradlorenz.kapp.schedule.web.dto.UpdateEnrollmentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns the raw JSON of {@code PATCH /api/schedule/me/enrollments/{enrollmentId}} into a
 * validated {@link UpdateEnrollmentRequest}. Mirrors {@code user-service}'s
 * {@code ProfilePatchReader}: a record cannot represent "the client left this field alone"
 * versus "the client cleared it", so this is the one place that reads the untyped tree.
 */
@Component
public class EnrollmentPatchReader {

    private static final Set<String> EDITABLE = Set.of("courseCode", "pensumItemCode", "courseName",
            "level", "credits", "totalHours", "group", "subgroup", "professor", "campus",
            "startDate", "endDate", "color");

    /** Weekly slots are only ever changed through the {@code /meetings} sub-resource. */
    private static final String MEETINGS = "meetings";

    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public UpdateEnrollmentRequest read(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new BusinessRuleException("The request body must be a JSON object");
        }
        if (body.isEmpty()) {
            throw new BusinessRuleException("Send at least one field to update");
        }

        List<ApiError.FieldIssue> issues = new ArrayList<>();
        rejectFieldsThatMayNotBeSent(body, issues);

        UpdateEnrollmentRequest patch = new UpdateEnrollmentRequest(
                requiredText(body, "courseCode", issues),
                requiredText(body, "pensumItemCode", issues),
                requiredText(body, "courseName", issues),
                requiredInt(body, "level", 1, 12, issues),
                requiredInt(body, "credits", 0, null, issues),
                requiredInt(body, "totalHours", 0, null, issues),
                requiredText(body, "group", issues),
                nullableText(body, "subgroup", issues),
                requiredText(body, "professor", issues),
                requiredText(body, "campus", issues),
                requiredDate(body, "startDate", issues),
                requiredDate(body, "endDate", issues),
                color(body, issues));

        if (!issues.isEmpty()) {
            throw new BusinessRuleException("Validation failed", issues);
        }
        return patch;
    }

    private void rejectFieldsThatMayNotBeSent(JsonNode body, List<ApiError.FieldIssue> issues) {
        Set<String> names = new LinkedHashSet<>();
        body.fieldNames().forEachRemaining(names::add);

        for (String name : names) {
            if (MEETINGS.equals(name)) {
                issues.add(new ApiError.FieldIssue(name,
                        "is not editable here; use the /meetings sub-resource instead"));
            } else if (!EDITABLE.contains(name)) {
                issues.add(new ApiError.FieldIssue(name, "is not a field of this resource"));
            }
        }
    }

    private Patched<String> requiredText(JsonNode body, String field, List<ApiError.FieldIssue> issues) {
        JsonNode node = body.get(field);
        if (node == null) {
            return Patched.absent();
        }
        if (node.isNull()) {
            issues.add(new ApiError.FieldIssue(field, "must not be null"));
            return Patched.absent();
        }
        if (!node.isTextual()) {
            issues.add(new ApiError.FieldIssue(field, "must be a string"));
            return Patched.absent();
        }
        String value = node.textValue();
        if (value.isBlank()) {
            issues.add(new ApiError.FieldIssue(field, "must not be blank"));
        }
        return Patched.of(value);
    }

    /** Only {@code subgroup} is nullable in this contract: absent leaves it, null clears it. */
    private Patched<String> nullableText(JsonNode body, String field, List<ApiError.FieldIssue> issues) {
        JsonNode node = body.get(field);
        if (node == null) {
            return Patched.absent();
        }
        if (node.isNull()) {
            return Patched.of(null);
        }
        if (!node.isTextual()) {
            issues.add(new ApiError.FieldIssue(field, "must be a string"));
            return Patched.absent();
        }
        return Patched.of(node.textValue());
    }

    private Patched<Integer> requiredInt(JsonNode body, String field, Integer min, Integer max,
                                         List<ApiError.FieldIssue> issues) {
        JsonNode node = body.get(field);
        if (node == null) {
            return Patched.absent();
        }
        if (node.isNull()) {
            issues.add(new ApiError.FieldIssue(field, "must not be null"));
            return Patched.absent();
        }
        if (!node.isIntegralNumber()) {
            issues.add(new ApiError.FieldIssue(field, "must be an integer"));
            return Patched.absent();
        }
        int value = node.intValue();
        if (min != null && value < min) {
            issues.add(new ApiError.FieldIssue(field, "must be at least " + min));
        } else if (max != null && value > max) {
            issues.add(new ApiError.FieldIssue(field, "must be at most " + max));
        }
        return Patched.of(value);
    }

    private Patched<LocalDate> requiredDate(JsonNode body, String field, List<ApiError.FieldIssue> issues) {
        JsonNode node = body.get(field);
        if (node == null) {
            return Patched.absent();
        }
        if (node.isNull()) {
            issues.add(new ApiError.FieldIssue(field, "must not be null"));
            return Patched.absent();
        }
        if (!node.isTextual()) {
            issues.add(new ApiError.FieldIssue(field, "must be a date string"));
            return Patched.absent();
        }
        try {
            return Patched.of(LocalDate.parse(node.textValue()));
        } catch (DateTimeParseException e) {
            issues.add(new ApiError.FieldIssue(field, "must be a valid date, for example 2026-07-27"));
            return Patched.absent();
        }
    }

    private Patched<String> color(JsonNode body, List<ApiError.FieldIssue> issues) {
        JsonNode node = body.get("color");
        if (node == null) {
            return Patched.absent();
        }
        if (node.isNull()) {
            issues.add(new ApiError.FieldIssue("color", "must not be null"));
            return Patched.absent();
        }
        if (!node.isTextual() || !COLOR.matcher(node.textValue()).matches()) {
            issues.add(new ApiError.FieldIssue("color", "must be a hex colour, for example #539392"));
            return Patched.absent();
        }
        return Patched.of(node.textValue());
    }
}
