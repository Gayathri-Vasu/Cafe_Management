package com.cafe.management.security;

import com.cafe.management.exception.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies an OpenID Connect ID token (the JWT returned by Google Identity
 * Services or Sign in with Apple).
 *
 * <p>The signature is always checked with {@code SHA256withRSA}, regardless of
 * the {@code alg} header the token claims. That single rule makes the classic
 * "alg: none" and HS256-key-confusion attacks impossible, because the header can
 * never influence which algorithm is used.</p>
 *
 * <p>The provider's public keys are downloaded from its JWKS endpoint and cached
 * for an hour; an unknown {@code kid} forces one immediate re-download so key
 * rotation does not break sign-in.</p>
 */
@Component
public class IdTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(IdTokenVerifier.class);

    private static final Duration JWKS_TTL = Duration.ofHours(1);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final long CLOCK_SKEW_SECONDS = 120;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final Map<String, CachedJwks> cache = new LinkedHashMap<>();

    /** The claims we actually need from a verified token. */
    public record VerifiedToken(String subject, String email, boolean emailVerified) {
    }

    /**
     * Verifies the token and returns its identity claims.
     *
     * @param idToken  the compact JWT produced by the provider's JavaScript SDK
     * @param jwksUri  the provider's public key set endpoint
     * @param issuers  the {@code iss} values the provider is allowed to have set
     *                 (Google uses two spellings of the same issuer)
     * @param audience the OAuth client id the token must be addressed to
     * @throws InvalidCredentialsException when the token is malformed, forged,
     *                                     expired, or meant for someone else
     */
    public VerifiedToken verify(String idToken, String jwksUri, List<String> issuers, String audience) {
        if (idToken == null || idToken.isBlank()) {
            throw new InvalidCredentialsException("The identity token is missing");
        }

        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new InvalidCredentialsException("The identity token is malformed");
        }

        JsonNode header = readJson(parts[0], "header");
        String kid = header.path("kid").asText(null);
        if (!"RS256".equals(header.path("alg").asText(null)) || kid == null || kid.isBlank()) {
            throw new InvalidCredentialsException("The identity token uses an unsupported signature");
        }

        PublicKey key = resolveKey(jwksUri, kid);
        verifySignature(parts, key);

        JsonNode claims = readJson(parts[1], "payload");
        validateClaims(claims, issuers, audience);

        String subject = claims.path("sub").asText(null);
        if (subject == null || subject.isBlank()) {
            throw new InvalidCredentialsException("The identity token has no subject");
        }

        return new VerifiedToken(
                subject,
                claims.path("email").asText(null),
                readEmailVerified(claims));
    }

    /** Google sends a real boolean, Apple sends the string {@code "true"} - accept both. */
    private boolean readEmailVerified(JsonNode claims) {
        JsonNode flag = claims.path("email_verified");
        return flag.isBoolean() ? flag.asBoolean() : "true".equalsIgnoreCase(flag.asText());
    }

    // ------------------------------------------------------------------
    // Signature + claim checks
    // ------------------------------------------------------------------

    private void verifySignature(String[] parts, PublicKey key) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));

            if (!signature.verify(Base64.getUrlDecoder().decode(parts[2]))) {
                throw new InvalidCredentialsException("The identity token signature is not valid");
            }
        } catch (InvalidCredentialsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("ID token signature check failed: {}", ex.getMessage());
            throw new InvalidCredentialsException("The identity token signature is not valid");
        }
    }

    private void validateClaims(JsonNode claims, List<String> issuers, String audience) {
        if (!issuers.contains(claims.path("iss").asText(null))) {
            throw new InvalidCredentialsException("The identity token was issued by an unexpected provider");
        }

        if (!audMatches(claims.path("aud"), audience)) {
            throw new InvalidCredentialsException("The identity token was issued for a different application");
        }

        long now = Instant.now().getEpochSecond();
        long expiry = claims.path("exp").asLong(0);
        if (expiry == 0 || expiry + CLOCK_SKEW_SECONDS < now) {
            throw new InvalidCredentialsException("The identity token has expired");
        }

        long notBefore = claims.path("nbf").asLong(0);
        if (notBefore != 0 && notBefore - CLOCK_SKEW_SECONDS > now) {
            throw new InvalidCredentialsException("The identity token is not valid yet");
        }
    }

    private boolean audMatches(JsonNode aud, String audience) {
        if (aud.isTextual()) {
            return aud.asText().equals(audience);
        }
        if (aud.isArray()) {
            for (JsonNode entry : aud) {
                if (entry.asText().equals(audience)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // JWKS handling
    // ------------------------------------------------------------------

    private PublicKey resolveKey(String jwksUri, String kid) {
        PublicKey key = keys(jwksUri, false).get(kid);
        if (key == null) {
            key = keys(jwksUri, true).get(kid);
        }
        if (key == null) {
            throw new InvalidCredentialsException("The identity provider does not recognise the signing key");
        }
        return key;
    }

    private synchronized Map<String, PublicKey> keys(String jwksUri, boolean forceRefresh) {
        CachedJwks cached = cache.get(jwksUri);
        boolean stale = cached == null || cached.isStale();
        if (!stale && !forceRefresh) {
            return cached.keys;
        }
        try {
            JsonNode document = downloadJwks(jwksUri);
            Map<String, PublicKey> parsed = new LinkedHashMap<>();
            for (JsonNode key : document.path("keys")) {
                if (!"RSA".equals(key.path("kty").asText())) {
                    continue;
                }
                String kid = key.path("kid").asText(null);
                if (kid != null) {
                    parsed.put(kid, toPublicKey(key));
                }
            }
            if (parsed.isEmpty()) {
                throw new IllegalStateException("the key set contained no RSA keys");
            }
            cache.put(jwksUri, new CachedJwks(parsed, Instant.now().plus(JWKS_TTL)));
            return parsed;
        } catch (Exception ex) {
            log.warn("Unable to download the JWKS from {}: {}", jwksUri, ex.getMessage());
            if (cached != null) {
                return cached.keys;
            }
            throw new InvalidCredentialsException("Unable to reach the identity provider to validate the token");
        }
    }

    private JsonNode downloadJwks(String jwksUri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(jwksUri))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return jsonMapper.readTree(response.body());
    }

    private PublicKey toPublicKey(JsonNode jwk) throws Exception {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.path("n").asText()));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.path("e").asText()));
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    private JsonNode readJson(String base64UrlPart, String what) {
        try {
            return jsonMapper.readTree(Base64.getUrlDecoder().decode(base64UrlPart));
        } catch (Exception ex) {
            throw new InvalidCredentialsException("The identity token " + what + " could not be read");
        }
    }

    private record CachedJwks(Map<String, PublicKey> keys, Instant fetchedAt) {
        boolean isStale() {
            return Instant.now().isAfter(fetchedAt);
        }
    }
}
