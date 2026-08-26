package co.edu.konradlorenz.kapp.auth;

import co.edu.konradlorenz.kapp.auth.domain.Credential;
import co.edu.konradlorenz.kapp.auth.domain.CredentialRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the security foundation the whole platform rests on: a token minted here is a
 * real RS256 JWT that verifies against the key published at
 * {@code /.well-known/jwks.json}. If this passes, the four resource servers can trust
 * tokens without any shared secret, which is what closes finding S1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class AuthServiceIntegrationTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    private static final String EMAIL = "brian@konradlorenz.edu.co";
    private static final String PASSWORD = "una-clave-larga-y-segura";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CredentialRepository credentials;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void seed() {
        credentials.deleteAll();
        credentials.save(new Credential(
                null, "507f1f77bcf86cd799439011", EMAIL,
                passwordEncoder.encode(PASSWORD),
                List.of("ROLE_STUDENT"),
                Credential.Status.ACTIVE, true, Credential.Provider.LOCAL,
                Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("issues a token that verifies against the published JWKS")
    void issuesVerifiableToken() throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value("507f1f77bcf86cd799439011"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_STUDENT"))
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(body).get("accessToken").asText();

        String jwksJson = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // The heart of the test: verify the signature using ONLY the public key the
        // other services would fetch. No shared secret is involved anywhere.
        RSAKey publicKey = (RSAKey) JWKSet.parse(jwksJson).getKeys().get(0);
        SignedJWT jwt = SignedJWT.parse(accessToken);

        assertThat(jwt.verify(new RSASSAVerifier(publicKey))).isTrue();
        assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("507f1f77bcf86cd799439011");
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("roles")).containsExactly("ROLE_STUDENT");
        assertThat(jwt.getJWTClaimsSet().getExpirationTime()).isAfter(java.util.Date.from(Instant.now()));
    }

    @Test
    @DisplayName("publishes only the public half of the key")
    void jwksNeverLeaksThePrivateKey() throws Exception {
        String jwksJson = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andReturn().getResponse().getContentAsString();

        // "d" is the RSA private exponent. Its presence would mean anyone could mint
        // administrator tokens.
        assertThat(jwksJson).doesNotContain("\"d\"");
        assertThat(JWKSet.parse(jwksJson).getKeys().get(0).isPrivate()).isFalse();
    }

    @Test
    @DisplayName("rejects a wrong password")
    void rejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"not-the-password"}""".formatted(EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("does not reveal whether an account exists")
    void doesNotLeakAccountExistence() throws Exception {
        String unknown = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@konradlorenz.edu.co","password":"whatever123"}"""))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String wrongPassword = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"not-the-password"}""".formatted(EMAIL)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Identical messages, so the endpoint cannot be used to enumerate accounts.
        assertThat(objectMapper.readTree(unknown).get("message").asText())
                .isEqualTo(objectMapper.readTree(wrongPassword).get("message").asText());
    }

    @Test
    @DisplayName("refuses a suspended account even with the right password")
    void refusesSuspendedAccount() throws Exception {
        credentials.deleteAll();
        credentials.save(new Credential(
                null, "u-suspended", EMAIL, passwordEncoder.encode(PASSWORD),
                List.of("ROLE_STUDENT"), Credential.Status.SUSPENDED, true,
                Credential.Provider.LOCAL, Instant.now(), Instant.now()));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(EMAIL, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("validation failures use the shared error envelope")
    void validationUsesSharedEnvelope() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("keeps protected endpoints closed to anonymous callers")
    void protectedEndpointsStillRequireAToken() throws Exception {
        mockMvc.perform(get("/api/auth/ping"))
                .andExpect(status().isUnauthorized());
    }
}
