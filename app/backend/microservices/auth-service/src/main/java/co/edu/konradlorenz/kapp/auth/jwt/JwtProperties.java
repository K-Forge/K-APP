package co.edu.konradlorenz.kapp.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * RSA signing material and token settings for {@code auth-service}.
 *
 * <p>Keys are PKCS#8 / X.509 PEM, base64-encoded so they survive being carried in an
 * environment variable. Generate a pair with:
 *
 * <pre>
 * openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem
 * openssl rsa -in private.pem -pubout -out public.pem
 * base64 -i private.pem | tr -d '\n'   # KAPP_JWT_PRIVATE_KEY
 * base64 -i public.pem  | tr -d '\n'   # KAPP_JWT_PUBLIC_KEY
 * </pre>
 *
 * <p>Leaving them unset generates an ephemeral pair at startup, which is convenient
 * locally and useless anywhere else: every restart invalidates every token in issue.
 * {@link RsaKeyProvider} logs a warning when that happens.
 *
 * @param privateKey base64 PKCS#8 PEM, or blank to generate an ephemeral pair
 * @param publicKey  base64 X.509 PEM, matching the private key
 * @param keyId      the {@code kid} published in the JWKS and stamped on every token,
 *                   so keys can be rotated without a flag day
 * @param ttl        how long an access token stays valid
 * @param issuer     the {@code iss} claim
 */
@ConfigurationProperties(prefix = "kapp.auth.jwt")
public record JwtProperties(
        String privateKey,
        String publicKey,
        String keyId,
        Duration ttl,
        String issuer
) {
    public JwtProperties {
        keyId = (keyId == null || keyId.isBlank()) ? "kapp-signing-key" : keyId;
        ttl = ttl == null ? Duration.ofHours(1) : ttl;
        issuer = (issuer == null || issuer.isBlank()) ? "https://kapp.konradlorenz.edu.co" : issuer;
    }

    public boolean hasConfiguredKeys() {
        return privateKey != null && !privateKey.isBlank()
                && publicKey != null && !publicKey.isBlank();
    }
}
