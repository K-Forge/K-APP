package co.edu.konradlorenz.kapp.semaphore.web.dto;

import co.edu.konradlorenz.kapp.semaphore.domain.Curriculum;
import co.edu.konradlorenz.kapp.semaphore.domain.CurriculumStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CurriculumDto(
        @NotBlank @Size(max = 20) String pensumCode,
        @NotBlank @Size(max = 20) String programCode,
        @NotBlank @Size(max = 100) String programName,
        @NotBlank @Size(max = 100) String faculty,
        @NotBlank @Size(max = 100) String reform,
        @NotNull CurriculumStatus status,
        @Min(0) int totalCredits,
        @Min(0) int totalHours,
        @Min(1) @Max(12) int levels,
        @NotEmpty @Valid List<CurriculumAreaDto> areas,
        @NotEmpty @Valid List<CurriculumCourseDto> courses
) {

    public static CurriculumDto from(Curriculum curriculum) {
        return new CurriculumDto(
                curriculum.pensumCode(),
                curriculum.programCode(),
                curriculum.programName(),
                curriculum.faculty(),
                curriculum.reform(),
                curriculum.status(),
                curriculum.totalCredits(),
                curriculum.totalHours(),
                curriculum.levels(),
                curriculum.areas().stream().map(CurriculumAreaDto::from).toList(),
                curriculum.coursesInDisplayOrder().stream().map(CurriculumCourseDto::from).toList());
    }

    public Curriculum toDomain() {
        return new Curriculum(
                pensumCode, programCode, programName, faculty, reform, status,
                totalCredits, totalHours, levels,
                areas.stream().map(CurriculumAreaDto::toDomain).toList(),
                courses.stream().map(CurriculumCourseDto::toDomain).toList());
    }
}
