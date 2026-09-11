package co.edu.konradlorenz.kapp.common.academic;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string is an academic period in the university's {@code YYYYS}
 * notation, e.g. {@code "20262"}. A {@code null} value passes, so combine with
 * {@code @NotNull} where the field is required.
 *
 * @see AcademicPeriod
 */
@Documented
@Constraint(validatedBy = ValidAcademicPeriod.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAcademicPeriod {

    String message() default "must be an academic period in YYYYS format, for example 20262";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidAcademicPeriod, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || AcademicPeriod.isValid(value);
        }
    }
}
