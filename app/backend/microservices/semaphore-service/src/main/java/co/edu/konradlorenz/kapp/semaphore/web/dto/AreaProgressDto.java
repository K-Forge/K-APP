package co.edu.konradlorenz.kapp.semaphore.web.dto;

/**
 * Credit progress within one knowledge area - one row of the summary.
 *
 * @param area         area code, as declared by the curriculum
 * @param creditsPassed credits of items in this area with status PASSED
 * @param creditsTotal  credits the curriculum declares for this area
 */
public record AreaProgressDto(
        String area,
        int creditsPassed,
        int creditsTotal
) {
}
