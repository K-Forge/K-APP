package co.edu.konradlorenz.kapp.user.web;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.user.domain.AcademicInfo;
import co.edu.konradlorenz.kapp.user.domain.Identification;
import co.edu.konradlorenz.kapp.user.web.dto.Patched;
import co.edu.konradlorenz.kapp.user.web.dto.ProfilePatch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns the raw JSON of {@code PATCH /api/users/me} into a validated {@link ProfilePatch}.
 *
 * <p>The endpoint binds a {@link JsonNode} rather than a record for one reason: a record
 * cannot represent the difference between a field the client omitted and a field the
 * client sent as {@code null}, and the contract gives those two opposite meanings. This
 * class is where that difference is read, and the only place in the service that sees
 * untyped JSON.
 *
 * <p>Unknown fields are rejected rather than ignored, matching
 * {@code additionalProperties: false} in the schema, and the four read-only fields get a
 * message that says why instead of the generic one - a client that PATCHes back a whole
 * profile object it just fetched should be told which fields it may not send, not simply
 * that something is unrecognised.
 */
@Component
public class ProfilePatchReader {

    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String PHONE = "phone";
    private static final String AVATAR_URL = "avatarUrl";
    private static final String IDENTIFICATION = "identification";
    private static final String ACADEMIC = "academic";

    private static final Set<String> EDITABLE =
            Set.of(FIRST_NAME, LAST_NAME, PHONE, AVATAR_URL, IDENTIFICATION, ACADEMIC);

    /** Owned elsewhere: e-mail by auth-service, role and activation by an administrator. */
    private static final Set<String> READ_ONLY = Set.of("email", "role", "active", "id");

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final int NAME_MAX = 50;
    private static final int PHONE_MAX = 16;
    private static final int AVATAR_URL_MAX = 500;

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ProfilePatchReader(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public ProfilePatch read(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new BusinessRuleException("The request body must be a JSON object");
        }
        if (body.isEmpty()) {
            throw new BusinessRuleException("Send at least one field to update");
        }

        List<ApiError.FieldIssue> issues = new ArrayList<>();
        rejectFieldsThatMayNotBeSent(body, issues);

        ProfilePatch patch = new ProfilePatch(
                requiredText(body, FIRST_NAME, NAME_MAX, issues),
                requiredText(body, LAST_NAME, NAME_MAX, issues),
                phone(body, issues),
                avatarUrl(body, issues),
                nested(body, IDENTIFICATION, Identification.class, issues),
                nested(body, ACADEMIC, AcademicInfo.class, issues));

        if (!issues.isEmpty()) {
            throw new BusinessRuleException("Validation failed", issues);
        }
        return patch;
    }

    private void rejectFieldsThatMayNotBeSent(JsonNode body, List<ApiError.FieldIssue> issues) {
        // LinkedHashSet so the order the client sent the fields in is the order it reads
        // them back, which makes a long error easier to match up against the request.
        Set<String> names = new LinkedHashSet<>();
        body.fieldNames().forEachRemaining(names::add);

        for (String name : names) {
            if (READ_ONLY.contains(name)) {
                issues.add(new ApiError.FieldIssue(name, readOnlyReason(name)));
            } else if (!EDITABLE.contains(name)) {
                issues.add(new ApiError.FieldIssue(name, "is not a field of this resource"));
            }
        }
    }

    private static String readOnlyReason(String field) {
        return switch (field) {
            case "email" -> "is owned by the authentication service and cannot be changed here";
            case "role" -> "is assigned at registration and cannot be changed here";
            case "active" -> "is changed through PATCH /api/users/{userId}/status";
            default -> "is assigned by the server and cannot be changed";
        };
    }

    /** A field the contract does not allow to be null: absent, or a non-blank string. */
    private Patched<String> requiredText(JsonNode body, String field, int maxLength,
                                         List<ApiError.FieldIssue> issues) {
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
        } else if (value.length() > maxLength) {
            issues.add(new ApiError.FieldIssue(field,
                    "must be at most " + maxLength + " characters"));
        }
        return Patched.of(value);
    }

    private Patched<String> phone(JsonNode body, List<ApiError.FieldIssue> issues) {
        JsonNode node = body.get(PHONE);
        if (node == null) {
            return Patched.absent();
        }
        if (node.isNull()) {
            return Patched.of(null);
        }
        if (!node.isTextual()) {
            issues.add(new ApiError.FieldIssue(PHONE, "must be a string"));
            return Patched.absent();
        }

        String value = node.textValue();
        if (value.length() > PHONE_MAX) {
            // 15 digits is the E.164 maximum; the sixteenth character is the leading plus.
            issues.add(new ApiError.FieldIssue(PHONE,
                    "must be at most " + PHONE_MAX + " characters"));
        } else if (!E164.matcher(value).matches()) {
            issues.add(new ApiError.FieldIssue(PHONE,
                    "must be in E.164 form, for example +573105551234"));
        }
        return Patched.of(value);
    }

    private Patched<String> avatarUrl(JsonNode body, List<ApiError.FieldIssue> issues) {
        JsonNode node = body.get(AVATAR_URL);
        if (node == null) {
            return Patched.absent();
        }
        if (node.isNull()) {
            return Patched.of(null);
        }
        if (!node.isTextual()) {
            issues.add(new ApiError.FieldIssue(AVATAR_URL, "must be a string"));
            return Patched.absent();
        }

        String value = node.textValue();
        if (value.length() > AVATAR_URL_MAX) {
            issues.add(new ApiError.FieldIssue(AVATAR_URL,
                    "must be at most " + AVATAR_URL_MAX + " characters"));
        } else if (!isAbsoluteUrl(value)) {
            issues.add(new ApiError.FieldIssue(AVATAR_URL, "must be an absolute URL"));
        }
        return Patched.of(value);
    }

    private static boolean isAbsoluteUrl(String value) {
        try {
            return new URI(value).isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * An object-valued field: absent, an explicit null that clears it, or a value that is
     * bound to its record and then run through bean validation, so the constraints on
     * {@link Identification} and {@link AcademicInfo} apply here exactly as they do on the
     * internal upsert.
     */
    private <T> Patched<T> nested(JsonNode body, String field, Class<T> type,
                                  List<ApiError.FieldIssue> issues) {
        JsonNode node = body.get(field);
        if (node == null) {
            return Patched.absent();
        }
        if (node.isNull()) {
            return Patched.of(null);
        }
        if (!node.isObject()) {
            issues.add(new ApiError.FieldIssue(field, "must be an object"));
            return Patched.absent();
        }

        T value;
        try {
            value = objectMapper.treeToValue(node, type);
        } catch (Exception e) {
            issues.add(new ApiError.FieldIssue(field, "is not a valid " + type.getSimpleName()));
            return Patched.absent();
        }

        for (ConstraintViolation<T> violation : validator.validate(value)) {
            issues.add(new ApiError.FieldIssue(
                    field + "." + violation.getPropertyPath(), violation.getMessage()));
        }
        return Patched.of(value);
    }
}
