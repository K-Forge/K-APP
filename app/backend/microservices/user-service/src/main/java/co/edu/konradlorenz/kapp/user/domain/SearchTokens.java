package co.edu.konradlorenz.kapp.user.domain;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Folds text so that an accent-insensitive search can still be served from an index.
 *
 * <h2>Why a derived field at all</h2>
 * The obvious implementation of "find Munoz when the user types munoz" is a
 * case-insensitive regex, {@code /muñoz/i}. It is wrong twice over: it does not match
 * {@code "Munoz"} spelled without the tilde, and the {@code i} flag makes the regex
 * unusable as an index bound, so every query becomes a collection scan.
 *
 * <p>Instead both sides are folded to the same normal form. Every word of the first name,
 * last name and e-mail is NFD-decomposed, stripped of combining marks and lowercased, and
 * the result is stored in a multikey-indexed array. {@code "Muñoz"} is written as
 * {@code "munoz"} and the query {@code "MUÑOZ"} is folded to {@code "munoz"} too, so the
 * comparison is a plain equality-shaped prefix.
 *
 * <h2>Why the query regex must be anchored and flagless</h2>
 * MongoDB can turn a regex into an index range only when it is anchored at the start with
 * {@code ^} and carries no {@code i} flag; anything else forces it to read every key.
 * Because both sides are already folded at write time, no flag is needed - which is
 * exactly what makes the index usable. See {@code UserDirectoryQueries}.
 *
 * <h2>Consequence the mobile team must know</h2>
 * This matches word <em>prefixes</em>, not infixes: {@code "varg"} finds Vargas,
 * {@code "unoz"} does not find Munoz. That is the right behaviour for a type-ahead, and
 * infix matching cannot be served from an index at all, so the interface must not promise
 * it.
 */
public final class SearchTokens {

    /** Combining marks left behind by NFD decomposition: the tilde of "ñ", the accent of "í". */
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    /** Anything that is not a letter or a digit separates one word from the next. */
    private static final Pattern SEPARATORS = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");

    /** Splits a user's query into the words they typed. */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Normalises one string to the form stored in {@code searchTokens}: decomposed,
     * stripped of accents, lowercased in a locale-independent way.
     *
     * <p>{@link Locale#ROOT} is not decoration. Under a Turkish locale the default
     * {@code toLowerCase()} maps {@code I} to a dotless {@code ı}, so a profile written on
     * one machine would stop matching a query folded on another.
     */
    public static String fold(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return COMBINING_MARKS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /**
     * Builds the token array for a profile: every word of the two names and of the
     * e-mail, plus the whole e-mail address, so a search for the full address also
     * matches by prefix.
     *
     * @return a sorted, duplicate-free list, so two profiles with the same words produce
     * byte-identical arrays and a document read back from Mongo compares equal
     */
    public static List<String> forProfile(String firstName, String lastName, String email) {
        SortedSet<String> tokens = new TreeSet<>();
        addWords(tokens, firstName);
        addWords(tokens, lastName);
        addWords(tokens, email);

        String wholeEmail = fold(email).strip();
        if (!wholeEmail.isEmpty()) {
            tokens.add(wholeEmail);
        }
        return List.copyOf(tokens);
    }

    /**
     * Folds a user's search string into the terms to match. Each returned term is a
     * prefix that some token of the profile must start with; several terms are combined
     * with AND, so {@code "vargas brian"} finds the same profile as {@code "brian
     * vargas"}.
     *
     * @return an empty list when the query holds nothing searchable, which the caller
     * must read as "matches nothing" rather than "no filter"
     */
    public static List<String> forQuery(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return WHITESPACE.splitAsStream(fold(query).strip())
                .filter(term -> !term.isEmpty())
                .distinct()
                .toList();
    }

    private static void addWords(Collection<String> target, String value) {
        String folded = fold(value);
        if (folded.isBlank()) {
            return;
        }
        for (String word : SEPARATORS.split(folded)) {
            if (!word.isEmpty()) {
                target.add(word);
            }
        }
    }

    private SearchTokens() {
    }
}
