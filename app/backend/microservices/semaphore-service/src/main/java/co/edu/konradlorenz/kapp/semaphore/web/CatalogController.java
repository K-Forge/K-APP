package co.edu.konradlorenz.kapp.semaphore.web;

import co.edu.konradlorenz.kapp.semaphore.security.AdminOnly;
import co.edu.konradlorenz.kapp.semaphore.security.CatalogRead;
import co.edu.konradlorenz.kapp.semaphore.service.CatalogService;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ProgramResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * The academic catalog: programs and curricula (pensums).
 *
 * <p>Reads are open to any authenticated role except {@code ROLE_GUEST}; mutations
 * require {@code ROLE_ADMIN}. See {@code docs/api/semaphore.openapi.yaml}.
 */
@RestController
@RequestMapping("/api/catalog")
@Validated
@Tag(name = "Catalog")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/programs")
    @CatalogRead
    @Operation(summary = "List academic programs")
    public List<ProgramResponse> listPrograms() {
        return catalog.listPrograms();
    }

    @GetMapping("/programs/{programCode}")
    @CatalogRead
    @Operation(summary = "Get one academic program")
    public ProgramResponse getProgram(@PathVariable String programCode) {
        return catalog.getProgram(programCode);
    }

    @GetMapping("/curricula/{pensumCode}")
    @CatalogRead
    @Operation(summary = "Get a full curriculum")
    public CurriculumDto getCurriculum(@PathVariable String pensumCode) {
        return catalog.getCurriculum(pensumCode);
    }

    @PutMapping("/curricula/{pensumCode}")
    @AdminOnly
    @Operation(summary = "Replace a curriculum")
    public CurriculumDto replaceCurriculum(@PathVariable String pensumCode,
                                            @Valid @RequestBody CurriculumDto body) {
        return catalog.replaceCurriculum(pensumCode, body);
    }

    @PostMapping("/curricula")
    @AdminOnly
    @Operation(summary = "Create a curriculum")
    public ResponseEntity<CurriculumDto> createCurriculum(@Valid @RequestBody CurriculumDto body) {
        CurriculumDto created = catalog.createCurriculum(body);
        return ResponseEntity.created(URI.create("/api/catalog/curricula/" + created.pensumCode()))
                .body(created);
    }

    @GetMapping("/curricula/{pensumCode}/courses")
    @CatalogRead
    @Operation(summary = "List the items of a curriculum")
    public List<CurriculumCourseDto> listCurriculumCourses(
            @PathVariable String pensumCode,
            @RequestParam(required = false) @Min(1) @Max(12) Integer level,
            @RequestParam(required = false) @Size(max = 10) String area,
            @RequestParam(required = false) Boolean isElectiveSlot) {
        return catalog.listCurriculumCourses(pensumCode, level, area, isElectiveSlot);
    }
}
