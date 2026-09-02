package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumStatus;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumAreaDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit coverage for the cross-field curriculum rules {@code createCurriculum} and
 * {@code replaceCurriculum} both depend on.
 */
class CurriculumValidatorTest {

    private final CurriculumValidator validator = new CurriculumValidator();

    @Test
    @DisplayName("a structurally sound curriculum passes")
    void validCurriculumPasses() {
        assertThat(validator).isNotNull();
        validator.validate(validCurriculum());
        // No exception: that is the assertion.
    }

    @Test
    @DisplayName("an elective slot with a non-null code is rejected")
    void electiveSlotWithCodeIsRejected() {
        CurriculumCourseDto badElective = new CurriculumCourseDto(
                "SHOULD-BE-NULL", "ELECTIVA_I", "Electiva I", 1, 3, 3, null, "ISA", true, List.of(), null);
        CurriculumDto dto = withCourses(List.of(badElective));

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getDetails())
                        .anySatisfy(issue -> assertThat(issue.field()).isEqualTo("courses[0].code")));
    }

    @Test
    @DisplayName("a fixed course with a null code is rejected")
    void fixedCourseWithoutCodeIsRejected() {
        CurriculumCourseDto badFixed = new CurriculumCourseDto(
                null, "1001", "Precalculo", 1, 3, 4, null, "CB", false, List.of(), null);
        CurriculumDto dto = withCourses(List.of(badFixed));

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getDetails())
                        .anySatisfy(issue -> assertThat(issue.field()).isEqualTo("courses[0].code")));
    }

    @Test
    @DisplayName("a course whose area is not declared by the curriculum is rejected")
    void undeclaredAreaIsRejected() {
        CurriculumCourseDto badArea = new CurriculumCourseDto(
                "10011", "1001", "Precalculo", 1, 3, 4, null, "NOPE", false, List.of(), null);
        CurriculumDto dto = withCourses(List.of(badArea));

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getDetails())
                        .anySatisfy(issue -> assertThat(issue.field()).isEqualTo("courses[0].area")));
    }

    @Test
    @DisplayName("a prerequisite that names no course code in the same document is rejected")
    void unknownPrerequisiteIsRejected() {
        CurriculumCourseDto badPrereq = new CurriculumCourseDto(
                "10024", "1102", "Calculo I", 2, 4, 5, null, "CB", false, List.of("GHOST-CODE"), null);
        CurriculumDto dto = withCourses(List.of(badPrereq));

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getDetails())
                        .anySatisfy(issue -> assertThat(issue.field()).isEqualTo("courses[0].prerequisites")));
    }

    @Test
    @DisplayName("a supplied totalHours that breaks weeklyHours * 16 is rejected")
    void wrongTotalHoursInvariantIsRejected() {
        CurriculumCourseDto badHours = new CurriculumCourseDto(
                "10011", "1001", "Precalculo", 1, 3, 4, 999, "CB", false, List.of(), null);
        CurriculumDto dto = withCourses(List.of(badHours));

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getDetails())
                        .anySatisfy(issue -> assertThat(issue.field()).isEqualTo("courses[0].totalHours")));
    }

    @Test
    @DisplayName("omitting totalHours on write is not an error - the server derives it")
    void omittedTotalHoursIsAccepted() {
        CurriculumCourseDto noHours = new CurriculumCourseDto(
                "10011", "1001", "Precalculo", 1, 3, 4, null, "CB", false, List.of(), null);
        validator.validate(withCourses(List.of(noHours)));
        // No exception: that is the assertion.
    }

    private static CurriculumDto withCourses(List<CurriculumCourseDto> courses) {
        return new CurriculumDto("1015", "506", "Ingenieria de Sistemas",
                "Facultad de Matematicas e Ingenierias", "Reforma 2018", CurriculumStatus.ACTIVE,
                142, 194, 9, List.of(area("CB"), area("ISA")), courses);
    }

    private static CurriculumDto validCurriculum() {
        CurriculumCourseDto fixed = new CurriculumCourseDto(
                "10011", "1001", "Precalculo", 1, 3, 4, 64, "CB", false, List.of(), "10011");
        CurriculumCourseDto dependent = new CurriculumCourseDto(
                "10024", "1102", "Calculo I", 2, 4, 5, 80, "CB", false, List.of("10011"), "10024");
        CurriculumCourseDto elective = new CurriculumCourseDto(
                null, "ELECTIVA_I", "Electiva I", 6, 3, 3, null, "ISA", true, List.of(), null);
        return withCourses(List.of(fixed, dependent, elective));
    }

    private static CurriculumAreaDto area(String code) {
        return new CurriculumAreaDto(code, "Area " + code, "#539392", 10, 10);
    }
}
