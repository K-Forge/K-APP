package co.edu.konradlorenz.kapp.semaphore.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcademicPlanRequest(
        @NotBlank @Size(max = 60) String name,
        @NotBlank @Size(max = 20) String pensumCode
) {
}
