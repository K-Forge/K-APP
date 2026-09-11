package co.edu.konradlorenz.kapp.user.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any authenticated role except {@code ROLE_GUEST}.
 *
 * <p>Spelled as a negation on purpose. "Everyone except guests" is the rule; writing it as
 * {@code hasAnyRole('STUDENT','PROFESSOR','ADMIN')} would mean that adding a role to KApp
 * silently locks it out of endpoints nobody remembered to update, which is the failure mode
 * that produced finding S2.
 *
 * <p>{@code ROLE_GUEST} is the one role denied, and it now means one specific thing: a
 * visitor holding a day pass, with no account behind it. The campus map is the only thing
 * such a token opens.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("!hasRole('GUEST')")
public @interface NotGuest {
}
