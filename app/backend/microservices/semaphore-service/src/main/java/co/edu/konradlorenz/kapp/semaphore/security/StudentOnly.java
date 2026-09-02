package co.edu.konradlorenz.kapp.semaphore.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Authorises {@code /api/semaphore/me/**}: {@code ROLE_STUDENT} only.
 *
 * <p>Narrower than {@link CatalogRead} on purpose. A professor or an admin has no
 * semaforo of their own to read - the published authorization table gives them no reach
 * here at all, not even read-only. An admin reaches a student's semaforo through
 * {@code GET /api/semaphore/{userId}} instead, which never accepts {@code "me"} as an
 * identity source.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('STUDENT')")
public @interface StudentOnly {
}
