package co.edu.konradlorenz.kapp.common.error;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Registers {@link GlobalExceptionHandler} in every service.
 *
 * <p>Auto-configuration rather than component scanning is the point of this class. The
 * handler lives in {@code co.edu.konradlorenz.kapp.common.error} while each application
 * class sits in a sibling package such as {@code co.edu.konradlorenz.kapp.auth}, and
 * Spring only scans downwards from the application class. Before this, the advice was
 * dead code and every domain exception came back as a plain HTTP 500.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class KappErrorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler kappGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
