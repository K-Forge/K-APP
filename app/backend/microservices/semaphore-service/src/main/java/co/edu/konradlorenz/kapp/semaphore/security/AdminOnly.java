package co.edu.konradlorenz.kapp.semaphore.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Authorises an administrative operation: catalog writes, and reading another
 * student's semaforo at {@code GET /api/semaphore/{userId}}. Requires {@code ROLE_ADMIN}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
public @interface AdminOnly {
}
