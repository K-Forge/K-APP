package co.edu.konradlorenz.kapp.auth.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Holds the RSA key pair that signs KApp access tokens, and exposes the public half as
 * a JWK set.
 *
 * <p>Asymmetric signing is a deliberate choice over a shared symmetric secret. With
 * HS512 the same value that verifies a token also mints one, so handing it to all seven
 * services would create seven places capable of forging an administrator token. Here
 * only this service holds the private key; everyone else fetches the public key from
 * {@code /.well-known/jwks.json} and can verify but never issue.
 *
 * <p>It also lines the project up for Microsoft Entra ID, which signs RS256 and publishes
 * a JWKS. Migrating then means pointing the services at a different {@code jwk-set-uri},
 * not rewriting how the services validate tokens.
 */
@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    private final RSAKey rsaKey;
    private final JWKSet publicJwkSet;

    public RsaKeyProvider(JwtProperties properties) {
        this.rsaKey = properties.hasConfiguredKeys()
                ? fromConfiguration(properties)
                : generateEphemeral(properties);
        this.publicJwkSet = new JWKSet(rsaKey.toPublicJWK());
    }

    public RSAKey signingKey() {
        return rsaKey;
    }

    /** The public half only. Never serialise {@link #signingKey()} to a client. */
    public JWKSet publicJwkSet() {
        return publicJwkSet;
    }

    private static RSAKey fromConfiguration(JwtProperties properties) {
        try {
            RSAPrivateKey privateKey = readPrivateKey(properties.privateKey());
            RSAPublicKey publicKey = readPublicKey(properties.publicKey());
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(properties.keyId())
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not read the configured RSA key pair. Check KAPP_JWT_PRIVATE_KEY and "
                            + "KAPP_JWT_PUBLIC_KEY: both must be base64-encoded PEM.", e);
        }
    }

    private static RSAKey generateEphemeral(JwtProperties properties) {
        log.warn("""
                No RSA key pair configured, generating an ephemeral one.
                Every restart will invalidate all tokens already issued, and a second
                instance of this service would sign with a different key. Acceptable for
                local development only. Set KAPP_JWT_PRIVATE_KEY and KAPP_JWT_PUBLIC_KEY
                for anything shared.""");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID(properties.keyId())
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate an ephemeral RSA key pair", e);
        }
    }

    private static RSAPrivateKey readPrivateKey(String base64Pem) throws Exception {
        byte[] der = pemBody(decode(base64Pem), "PRIVATE KEY");
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static RSAPublicKey readPublicKey(String base64Pem) throws Exception {
        byte[] der = pemBody(decode(base64Pem), "PUBLIC KEY");
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    private static String decode(String base64) {
        return new String(Base64.getDecoder().decode(base64.trim()));
    }

    private static byte[] pemBody(String pem, String label) {
        String body = pem
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}
