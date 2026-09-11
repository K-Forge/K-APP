package co.edu.konradlorenz.kapp.semaphore.domain;

/**
 * One item of a student's semaforo: the status of a single pensum item, plus the elective
 * resolution when the item is a slot.
 *
 * <p>The entry mirrors a {@link PensumCourse} one-to-one and is keyed by the same
 * {@code pensumItemCode}. {@code code} is copied from the pensum item so a client can
 * render the grid without a second lookup, and stays {@code null} for an elective slot
 * even after the slot has been resolved - the real course lives in {@code resolvedCode},
 * because the slot itself is what the pensum contains.
 *
 * @param code           course code of the pensum item; null for an elective slot
 * @param pensumItemCode identifier of the pensum item this entry tracks
 * @param status         stored status; never null
 * @param period         academic period in YYYYS notation, or null
 * @param grade          final mark on the university's 0..50 integer scale, or null
 * @param resolvedCode   code of the real course a slot was resolved to, or null
 * @param resolvedName   name of that course, denormalised for display, or null
 */
public record StudentProgressCourse(
        String code,
        String pensumItemCode,
        CourseStatus status,
        String period,
        Integer grade,
        String resolvedCode,
        String resolvedName
) {

    /** The university's grade scale. Not 0..5 and not 0..100: the SINU schema says 0..50. */
    public static final int MIN_GRADE = 0;
    public static final int MAX_GRADE = 50;

    /** @return a PENDING entry for a freshly materialised pensum item */
    public static StudentProgressCourse pendingFor(PensumCourse item) {
        return new StudentProgressCourse(
                item.code(), item.pensumItemCode(), CourseStatus.PENDING, null, null, null, null);
    }

    public String addressableCode() {
        return code != null ? code : pensumItemCode;
    }

    public StudentProgressCourse withStatus(CourseStatus newStatus, Integer newGrade, String newPeriod) {
        return new StudentProgressCourse(
                code, pensumItemCode, newStatus, newPeriod, newGrade, resolvedCode, resolvedName);
    }

    public StudentProgressCourse withResolution(String newResolvedCode, String newResolvedName) {
        return new StudentProgressCourse(
                code, pensumItemCode, status, period, grade, newResolvedCode, newResolvedName);
    }
}
