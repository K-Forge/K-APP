package co.edu.konradlorenz.kapp.common.security;

import co.edu.konradlorenz.kapp.common.error.ApiError;
import co.edu.konradlorenz.kapp.common.feign.InternalTokenInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Checks the shared secret guarding {@code /internal/**}.
 *
 * <p>Not a bean, and not registered with the servlet container: it is constructed by
 * {@code InternalApiSecurityConfig} and added to that one chain. Declared as a
 * {@code @Component} it would be auto-registered as a plain servlet filter and would run
 * on every request in the application, including the ones it knows nothing about.
 *
 * <h2>Why both sides are hashed before they are compared</h2>
 * {@link MessageDigest#isEqual} is constant time for equal-length inputs, but a naive
 * comparison still leaks the length of the expected secret through how long it takes to
 * fail. Hashing both sides first means the comparison always runs over 32 bytes whatever
 * was supplied, so neither the content nor the length of the secret is observable from
 * outside.
 *
 * <h2>Failing closed</h2>
 * With no secret configured the filter rejects everything and says so once at startup.
 * The alternative - treating "unset" as "no check" - turns a missing environment variable
 * into an open write endpoint, and the deployment that forgot it is exactly the one where
 * nobody is watching the logs.
 */
public class InternalTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(InternalTokenAuthenticationFilter.class);

    private final byte[] expectedDigest;
    private final ObjectMapper objectMapper;

    public InternalTokenAuthenticationFilter(String configuredToken, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.expectedDigest = (configuredToken == null || configuredToken.isBlank())
                ? null
                : sha256(configuredToken);

        if (this.expectedDigest == null) {
            log.warn("KAPP_INTERNAL_TOKEN is not set. Every request to /internal/** will be "
                    + "rejected with 401, so registration cannot create profiles. Set it to the "
                    + "same value in auth-service and user-service.");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!isAuthorised(request.getHeader(InternalTokenInterceptor.HEADER))) {
            writeUnauthorised(response, request.getRequestURI());
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isAuthorised(String supplied) {
        if (expectedDigest == null || supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(expectedDigest, sha256(supplied));
    }

    private void writeUnauthorised(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // The same envelope the shared entry point writes, and the same wording: a caller
        // learns that it was not authenticated, never whether the token was absent, the
        // wrong length or merely wrong.
        ApiError body = ApiError.of(HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Authentication required", path,
                List.of());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required of every Java platform; this cannot happen.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
