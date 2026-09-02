package co.edu.konradlorenz.kapp.semaphore.web.dto;

import co.edu.konradlorenz.kapp.semaphore.domain.CourseStatus;
import co.edu.konradlorenz.kapp.semaphore.domain.StudentProgressCourse;

public record StudentProgressCourseDto(
        String code,
        String pensumItemCode,
        CourseStatus status,
        String period,
        Integer grade,
        String resolvedCode,
        String resolvedName
) {

    public static StudentProgressCourseDto from(StudentProgressCourse entry) {
        return new StudentProgressCourseDto(entry.code(), entry.pensumItemCode(), entry.status(),
                entry.period(), entry.grade(), entry.resolvedCode(), entry.resolvedName());
    }
}
