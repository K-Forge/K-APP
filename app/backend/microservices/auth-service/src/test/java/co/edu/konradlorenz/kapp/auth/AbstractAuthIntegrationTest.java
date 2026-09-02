package co.edu.konradlorenz.kapp.auth;

import co.edu.konradlorenz.kapp.auth.client.InternalUserUpsert;
import co.edu.konradlorenz.kapp.auth.client.UserProfileClient;
import co.edu.konradlorenz.kapp.auth.client.UserProfileView;
import co.edu.konradlorenz.kapp.auth.domain.Credential;
import co.edu.konradlorenz.kapp.auth.domain.CredentialRepository;
import co.edu.konradlorenz.kapp.auth.jwt.JwtIssuer;
import co.edu.konradlorenz.kapp.auth.service.VerificationMailer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Shared fixture for every auth-service integration test.
 *
 * <p>One real MongoDB via Testcontainers - the {@code @Container} field is inherited, so
 * every subclass reuses the same instance rather than starting one each - and a mocked
 * {@link UserProfileClient}, per the instruction to test registration without standing up
 * a second service. The mock answers every {@code upsert} with a fresh random profile id
 * unless a test overrides it, which covers every test that only needs registration to
 * succeed; the tests that care about user-service being unreachable restub it themselves.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        // InternalUserClientConfig refuses a genuinely blank token at bean creation.
        // The Feign client this token would decorate is mocked below, so the value
        // itself is never sent anywhere; it only has to be non-blank for the context
        // to start.
        "kapp.internal.token=test-internal-token"
})
abstract class AbstractAuthIntegrationTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    protected static final String INSTITUTIONAL_DOMAIN = "@konradlorenz.edu.co";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected CredentialRepository credentials;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected MongoTemplate mongoTemplate;
    @Autowired
    protected JwtIssuer jwtIssuer;

    @MockitoBean
    protected UserProfileClient userProfileClient;

    /**
     * Mocked so verification tests can capture the raw token: the database only ever
     * holds its SHA-256 hash, so there is no other way to recover a value to verify with.
     * A no-op by default, which is also the correct behaviour for every test that does
     * not care about e-mail delivery at all.
     */
    @MockitoBean
    protected VerificationMailer verificationMailer;

    @BeforeEach
    void resetSharedFixture() {
        credentials.deleteAll();
        // Only test-seeded codes, never the Mongock-seeded demo codes: those are shared
        // across the whole suite and other tests rely on their quotas still standing.
        mongoTemplate.getCollection("invitation_codes")
                .deleteMany(new Document("code", new Document("$regex", "^KL-TEST-")));

        when(userProfileClient.upsert(any())).thenAnswer(invocation -> {
            InternalUserUpsert body = invocation.getArgument(0, InternalUserUpsert.class);
            return new UserProfileView(UUID.randomUUID().toString(), body.email());
        });
    }

    /** Persists a credential directly, bypassing registration, for login/verify fixtures. */
    protected Credential seedCredential(String email, String rawPassword, List<String> roles,
                                        Credential.Status status, boolean emailVerified) {
        Instant now = Instant.now();
        return credentials.save(new Credential(null, UUID.randomUUID().toString(), email,
                passwordEncoder.encode(rawPassword), roles, status, emailVerified,
                Credential.Provider.LOCAL, null, null, now, now));
    }

    /** A signed access token for an identity that need not exist in the database at all -
     * the authorization matrix only cares what the token's {@code roles} claim says. */
    protected String bearerFor(String userId, String email, String... roles) {
        return "Bearer " + jwtIssuer.issue(userId, email, List.of(roles)).accessToken();
    }

    /** The most recent raw verification token handed to the (mocked) mailer for this address. */
    protected String lastVerificationTokenFor(String email) {
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(verificationMailer, org.mockito.Mockito.atLeastOnce())
                .sendVerificationLink(org.mockito.ArgumentMatchers.eq(email), captor.capture());
        return captor.getValue();
    }

    /** Inserts an invitation code straight into MongoDB, bypassing the redemption guard. */
    protected void seedInvitationCode(String code, String role, int maxUses, int timesUsed,
                                      boolean active, Instant expiresAt) {
        Instant now = Instant.now();
        Document doc = new Document()
                .append("code", code)
                .append("role", role)
                .append("maxUses", maxUses)
                .append("timesUsed", timesUsed)
                .append("active", active)
                .append("expiresAt", expiresAt == null ? null : Date.from(expiresAt))
                .append("createdAt", Date.from(now))
                .append("updatedAt", Date.from(now));
        mongoTemplate.getCollection("invitation_codes").insertOne(doc);
    }
}
