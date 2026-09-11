package co.edu.konradlorenz.kapp.semaphore.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Authorises a catalog read: any authenticated role except {@code ROLE_GUEST}.
 *
 * <p>The published rule for {@code /api/catalog/**} reads is "any authenticated role
 * except GUEST", not "STUDENT, PROFESSOR or ADMIN". Spelling it as a negation, rather
 * than enumerating three roles, means a fourth role added later inherits catalog read
 * access automatically instead of silently being locked out until someone remembers to
 * update every {@code @PreAuthorize}. {@code ROLE_GUEST} is the one role denied
 * everywhere in this service, catalog reads included.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("!hasRole('GUEST')")
public @interface CatalogRead {
}
