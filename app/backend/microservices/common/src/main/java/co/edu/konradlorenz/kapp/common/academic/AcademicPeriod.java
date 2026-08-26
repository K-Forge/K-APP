package co.edu.konradlorenz.kapp.common.academic;

import java.util.regex.Pattern;

/**
 * An academic period in the university's own notation: four digits of year followed by
 * the semester, {@code 1} or {@code 2}. For example {@code "20262"} is the second
 * semester of 2026.
 *
 * <p>This format is not invented here. It is what the university's academic system
 * prints on the student's timetable report, so adopting it verbatim means a future
 * synchronisation needs no translation.
 *
 * <p>It lives in {@code common} because two different services store it -
 * {@code schedule-service} on a timetable and {@code semaphore-service} on each course
 * a student has taken. Left to drift, one would write {@code "2026-2"} and the other
 * {@code "20262"}, and the two could never be joined.
 */
public record AcademicPeriod(int year, int semester) implements Comparable<AcademicPeriod> {

    public static final String PATTERN = "^\\d{4}[12]$";
    private static final Pattern COMPILED = Pattern.compile(PATTERN);

    public AcademicPeriod {
        if (year < 1900 || year > 2999) {
            throw new IllegalArgumentException("Academic period year out of range: " + year);
        }
        if (semester != 1 && semester != 2) {
            throw new IllegalArgumentException("Academic period semester must be 1 or 2: " + semester);
        }
    }

    /**
     * @param value a period such as {@code "20262"}
     * @throws IllegalArgumentException if the value does not match {@link #PATTERN}
     */
    public static AcademicPeriod parse(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException(
                    "Invalid academic period '" + value + "'. Expected YYYYS, for example 20262.");
        }
        return new AcademicPeriod(
                Integer.parseInt(value.substring(0, 4)),
                Integer.parseInt(value.substring(4)));
    }

    public static boolean isValid(String value) {
        return value != null && COMPILED.matcher(value).matches();
    }

    public AcademicPeriod next() {
        return semester == 1 ? new AcademicPeriod(year, 2) : new AcademicPeriod(year + 1, 1);
    }

    public AcademicPeriod previous() {
        return semester == 2 ? new AcademicPeriod(year, 1) : new AcademicPeriod(year - 1, 2);
    }

    @Override
    public int compareTo(AcademicPeriod other) {
        int byYear = Integer.compare(year, other.year);
        return byYear != 0 ? byYear : Integer.compare(semester, other.semester);
    }

    /** @return the canonical wire format, e.g. {@code "20262"} */
    @Override
    public String toString() {
        return "%04d%d".formatted(year, semester);
    }
}
