package co.edu.konradlorenz.kapp.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The folding rules, tested without Spring or MongoDB because they are pure functions and
 * because everything else about the search rests on them.
 */
class SearchTokensTest {

    @Test
    @DisplayName("strips the accents that make the same name two different strings")
    void stripsAccents() {
        assertThat(SearchTokens.fold("Muñoz")).isEqualTo("munoz");
        assertThat(SearchTokens.fold("Rodríguez Peña")).isEqualTo("rodriguez pena");
        assertThat(SearchTokens.fold("María Fernanda")).isEqualTo("maria fernanda");
    }

    @Test
    @DisplayName("folds an accented name and its unaccented spelling to the same token")
    void accentedAndUnaccentedSpellingsAgree() {
        assertThat(SearchTokens.fold("Muñoz")).isEqualTo(SearchTokens.fold("Munoz"));
    }

    @Test
    @DisplayName("lowercases independently of the default locale")
    void lowercasesIndependentlyOfLocale() {
        Locale original = Locale.getDefault();
        try {
            // Turkish maps a capital I to a dotless small i. A profile written on a
            // machine set to this locale would stop matching a query folded on any other,
            // which is the whole reason fold() pins Locale.ROOT.
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertThat(SearchTokens.fold("ISABEL")).isEqualTo("isabel");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    @DisplayName("indexes every word of both names and of the e-mail")
    void indexesEveryWord() {
        List<String> tokens = SearchTokens.forProfile(
                "Brian Steven", "Vargas Clavijo", "brian.vargasc@konradlorenz.edu.co");

        assertThat(tokens).contains("brian", "steven", "vargas", "clavijo",
                "vargasc", "konradlorenz", "edu", "co");
    }

    @Test
    @DisplayName("keeps the whole e-mail as a token so the full address matches too")
    void keepsTheWholeEmail() {
        assertThat(SearchTokens.forProfile("Brian", "Vargas", "brian.vargasc@konradlorenz.edu.co"))
                .contains("brian.vargasc@konradlorenz.edu.co");
    }

    @Test
    @DisplayName("returns tokens sorted and deduplicated, so equal profiles store equal arrays")
    void tokensAreSortedAndUnique() {
        List<String> tokens = SearchTokens.forProfile("Ana", "Ana", "ana@ana.co");

        assertThat(tokens).isSorted().doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("splits a multi-word query into one term per word")
    void splitsAQueryIntoTerms() {
        assertThat(SearchTokens.forQuery("  Vargas   Clavijo ")).containsExactly("vargas", "clavijo");
    }

    @Test
    @DisplayName("folds the query the same way the stored tokens were folded")
    void foldsTheQueryTheSameWay() {
        assertThat(SearchTokens.forQuery("MUÑOZ")).containsExactly("munoz");
    }

    @Test
    @DisplayName("yields no terms for a query with nothing searchable in it")
    void yieldsNoTermsForAnEmptyQuery() {
        assertThat(SearchTokens.forQuery(null)).isEmpty();
        assertThat(SearchTokens.forQuery("   ")).isEmpty();
    }

    @Test
    @DisplayName("tolerates a profile with no e-mail rather than failing to build tokens")
    void toleratesMissingFields() {
        assertThat(SearchTokens.forProfile("Ana", null, null)).containsExactly("ana");
    }
}
