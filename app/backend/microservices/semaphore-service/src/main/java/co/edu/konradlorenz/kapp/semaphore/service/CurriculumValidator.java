package co.edu.konradlorenz.kapp.semaphore.service;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.error.BusinessRuleException;
import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumCourse;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumAreaDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The cross-field rules {@code CurriculumDto}'s bean validation cannot see on its own,
 * because each one depends on more than one field or on a sibling item in the same
 * document. Shared by {@code createCurriculum} and {@code replaceCurriculum} so an admin
 * cannot create an inconsistent pensum through one path that the other would refuse.
 */
@Component
public class CurriculumValidator {

    /**
     * @throws BusinessRuleException with one {@link ApiError.FieldIssue} per violation,
     *                                naming the offending item by its position, so an
     *                                admin UI can point at the exact row
     */
    public void validate(CurriculumDto dto) {
        List<ApiError.FieldIssue> issues = new ArrayList<>();

        Set<String> areaCodes = dto.areas().stream().map(CurriculumAreaDto::code).collect(Collectors.toSet());
        Set<String> courseCodes = dto.courses().stream()
                .map(CurriculumCourseDto::code)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<CurriculumCourseDto> courses = dto.courses();
        for (int i = 0; i < courses.size(); i++) {
            CurriculumCourseDto course = courses.get(i);
            String prefix = "courses[%d]".formatted(i);

            if (course.isElectiveSlot() && course.code() != null) {
                issues.add(new ApiError.FieldIssue(prefix + ".code",
                        "must be null for an elective slot"));
            }
            if (!course.isElectiveSlot() && course.code() == null) {
                issues.add(new ApiError.FieldIssue(prefix + ".code",
                        "must not be null for a fixed course"));
            }
            if (!areaCodes.contains(course.area())) {
                issues.add(new ApiError.FieldIssue(prefix + ".area",
                        "'%s' is not one of the declared areas".formatted(course.area())));
            }
            if (course.totalHours() != null
                    && course.totalHours() != course.weeklyHours() * CurriculumCourse.WEEKS_PER_SEMESTER) {
                issues.add(new ApiError.FieldIssue(prefix + ".totalHours",
                        "must equal weeklyHours * %d when supplied".formatted(CurriculumCourse.WEEKS_PER_SEMESTER)));
            }
            for (String prerequisite : course.prerequisites()) {
                if (!courseCodes.contains(prerequisite)) {
                    issues.add(new ApiError.FieldIssue(prefix + ".prerequisites",
                            "'%s' is not the code of any course in this curriculum".formatted(prerequisite)));
                }
            }
        }

        if (!issues.isEmpty()) {
            throw new BusinessRuleException("Curriculum failed validation", issues);
        }
    }
}
