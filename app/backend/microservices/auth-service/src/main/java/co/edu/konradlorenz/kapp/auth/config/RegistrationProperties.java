package co.edu.konradlorenz.kapp.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Registration and e-mail verification settings.
 *
 * <p>Bound from {@code kapp.auth.registration.*}, which {@code application.yml} fills from
 * the environment variables {@code docker-compose.yml} already sets.
 *
 * @param allowedEmailDomain       the domain {@code POST /auth/register} demands. Guests are
 *                                 unrestricted and land on {@code ROLE_GUEST} instead.
 * @param requireEmailVerification whether a new account must confirm its address before it
 *                                 can sign in. Off by default because there is no SMTP relay
 *                                 yet and registration is the front door of the product;
 *                                 accounts are then created {@code ACTIVE} while
 *                                 {@code emailVerified} still says {@code false}, because
 *                                 nothing was actually verified. Turn it on the moment mail
 *                                 works.
 * @param verificationLinkBase     the URL the verification e-mail points at; the token is
 *                                 appended as {@code ?token=...}
 * @param verificationTtl          how long a verification token stays valid
 * @param mailFrom                 the From address on verification e-mails
 * @param defaultPensumCode        curriculum version assigned to a newly registered student.
 *                                 The registration contract does not carry one, and
 *                                 user-service requires it on the academic record.
 * @param defaultCurrentLevel      semester a newly registered student starts on
 */
@ConfigurationProperties(prefix = "kapp.auth.registration")
public record RegistrationProperties(
        String allowedEmailDomain,
        boolean requireEmailVerification,
        String verificationLinkBase,
        Duration verificationTtl,
        String mailFrom,
        String defaultPensumCode,
        Integer defaultCurrentLevel
) {
    public RegistrationProperties {
        allowedEmailDomain = orDefault(allowedEmailDomain, "konradlorenz.edu.co").toLowerCase();
        verificationLinkBase = orDefault(verificationLinkBase, "http://localhost:8080/auth/verify");
        verificationTtl = verificationTtl == null ? Duration.ofHours(24) : verificationTtl;
        mailFrom = orDefault(mailFrom, "no-reply@konradlorenz.edu.co");
        defaultPensumCode = orDefault(defaultPensumCode, "1015");
        defaultCurrentLevel = defaultCurrentLevel == null ? 1 : defaultCurrentLevel;
    }

    /** The account status a freshly registered credential gets. */
    public boolean gatesSignInOnVerification() {
        return requireEmailVerification;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
