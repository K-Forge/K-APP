package co.edu.konradlorenz.kapp.auth.service;

import co.edu.konradlorenz.kapp.auth.config.RegistrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Delivers the verification link, or writes it to the log when there is no relay.
 *
 * <p>The university has not provisioned a mailbox yet, so {@code SPRING_MAIL_HOST} is
 * blank on every developer laptop. Rather than making registration depend on mail that
 * cannot be sent, a blank host switches this to log mode: the link is printed at INFO and
 * a tester copies it. That is what makes
 * {@code KAPP_REQUIRE_EMAIL_VERIFICATION=true} safe to turn on locally, and it is why the
 * flag and the relay are separate settings.
 *
 * <p>Sending is best effort by design. A relay that is down must not turn a successful
 * registration into a failed one - the account exists, and
 * {@code POST /auth/verify/resend} is the recovery path.
 */
@Component
public class VerificationMailer {

    private static final Logger log = LoggerFactory.getLogger(VerificationMailer.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final RegistrationProperties properties;
    private final String mailHost;

    public VerificationMailer(ObjectProvider<JavaMailSender> mailSender,
                              RegistrationProperties properties,
                              @Value("${spring.mail.host:}") String mailHost) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.mailHost = mailHost;
    }

    /**
     * @param email    where the link goes
     * @param rawToken the token itself. This is the only place it exists outside the
     *                 response to the request that minted it - the database holds a
     *                 SHA-256 hash of it and nothing else.
     */
    public void sendVerificationLink(String email, String rawToken) {
        String link = properties.verificationLinkBase()
                + (properties.verificationLinkBase().contains("?") ? "&" : "?")
                + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

        if (!hasRelay()) {
            log.info("""
                    No SMTP relay configured (spring.mail.host is blank), so the verification \
                    e-mail for {} was not sent. Use this link to confirm the address: {}""",
                    email, link);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.mailFrom());
            message.setTo(email);
            message.setSubject("Confirma tu correo - KApp Konrad Lorenz");
            message.setText("""
                    Hola,

                    Confirma tu correo para activar tu cuenta de KApp:

                    %s

                    Si no creaste esta cuenta, ignora este mensaje.

                    K-Forge - Fundacion Universitaria Konrad Lorenz""".formatted(link));
            mailSender.getObject().send(message);
            log.info("Verification e-mail sent to {}", email);
        } catch (Exception e) {
            // Never fails the registration that triggered it. The account exists and
            // /auth/verify/resend is the way back.
            log.error("Could not send the verification e-mail to {}", email, e);
        }
    }

    public boolean hasRelay() {
        return mailHost != null && !mailHost.isBlank() && mailSender.getIfAvailable() != null;
    }
}
