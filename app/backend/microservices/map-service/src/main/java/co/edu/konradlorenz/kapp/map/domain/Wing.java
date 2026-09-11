package co.edu.konradlorenz.kapp.map.domain;

/**
 * Which arm of a floor a space sits in.
 *
 * <p>The central building's rooms come in threes: {@code 301}, {@code 301-N} and
 * {@code 301-S} are three different rooms on the same floor, and a student sent to "301"
 * without the wing is standing in the wrong place a third of the time.
 *
 * <p>This is a FIELD, not a suffix parsed out of the code. Search has to be able to filter
 * by wing and the interface has to group by it, and deriving either from the last two
 * characters of a string would break the first time a building names its wings anything
 * else. A building with a single arm leaves it null.
 */
public enum Wing {
    NORTE,
    SUR,
    CENTRAL
}
