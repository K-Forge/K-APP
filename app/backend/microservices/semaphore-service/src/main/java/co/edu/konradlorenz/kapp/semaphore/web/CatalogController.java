package co.edu.konradlorenz.kapp.semaphore.web;

import co.edu.konradlorenz.kapp.semaphore.security.AdminOnly;
import co.edu.konradlorenz.kapp.semaphore.security.CatalogRead;
import co.edu.konradlorenz.kapp.semaphore.service.CatalogService;
import co.edu.konradlorenz.kapp.semaphore.service.CurriculumCsvImporter;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumCourseDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumDto;
import co.edu.konradlorenz.kapp.semaphore.web.dto.CurriculumImportReport;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ProgramRequest;
import co.edu.konradlorenz.kapp.semaphore.web.dto.ProgramResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
    private final CurriculumCsvImporter importer;

    public CatalogController(CatalogService catalog, CurriculumCsvImporter importer) {
        this.catalog = catalog;
        this.importer = importer;
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

    @PostMapping("/programs")
    @AdminOnly
    @Operation(summary = "Create an academic program")
    public ResponseEntity<ProgramResponse> createProgram(@Valid @RequestBody ProgramRequest body) {
        ProgramResponse created = catalog.createProgram(body);
        return ResponseEntity.created(URI.create("/api/catalog/programs/" + created.code()))
                .body(created);
    }

    @PutMapping("/programs/{programCode}")
    @AdminOnly
    @Operation(summary = "Replace an academic program")
    public ProgramResponse replaceProgram(@PathVariable String programCode,
                                           @Valid @RequestBody ProgramRequest body) {
        return catalog.replaceProgram(programCode, body);
    }

    @DeleteMapping("/programs/{programCode}")
    @AdminOnly
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an academic program")
    public void deleteProgram(@PathVariable String programCode) {
        catalog.deleteProgram(programCode);
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

    @DeleteMapping("/curricula/{pensumCode}")
    @AdminOnly
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a curriculum")
    public void deleteCurriculum(@PathVariable String pensumCode) {
        catalog.deleteCurriculum(pensumCode);
    }

    /**
     * Bulk load of pensums from a spreadsheet export.
     *
     * <p>Read as UTF-8 explicitly rather than through the platform default: these files come
     * off machines whose default may be anything, and a mis-decoded "Matemáticas" would be
     * stored wrong and only noticed by a student reading their own semáforo.
     */
    @PostMapping(value = "/curricula/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AdminOnly
    @Operation(summary = "Import pensums in bulk from a CSV",
            description = "Validates the whole file first; nothing is written unless everything "
                    + "passes. Use dryRun=true to check a file without importing it.")
    public CurriculumImportReport importCurricula(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) throws IOException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return importer.importFrom(reader, dryRun);
        }
    }

    @GetMapping("/curricula/{pensumCode}/courses")
    @CatalogRead
    @Operation(summary = "List the items of a curriculum")
    public List<CurriculumCourseDto> listCurriculumCourses(
            @PathVariable String pensumCode,
            @RequestParam(required = false) @Min(1) @Max(12) Integer level,
            @RequestParam(required = false) @Size(max = 20) String area,
            @RequestParam(required = false) Boolean isElectiveSlot) {
        return catalog.listCurriculumCourses(pensumCode, level, area, isElectiveSlot);
    }
}
