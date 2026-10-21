package com.cafe.management.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Creates and validates JSON Web Tokens (HS256).
 *
 * <p>A token is a signed string {@code header.payload.signature} that carries the
 * user's email (subject) and role (custom claim). Because it is signed with a
 * server-side secret, the client cannot tamper with it.</p>
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Builds a signed token for the given user. */
    public String generateToken(String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    /** Reads the subject (email) from a token. */
    public String extractUsername(String token) {
        return parseClaims(token).getPayload().getSubject();
    }

    /** True when the token belongs to the user, is well-formed and not expired. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token).getPayload();
            boolean sameUser = claims.getSubject() != null
                    && claims.getSubject().equalsIgnoreCase(userDetails.getUsername());
            boolean notExpired = claims.getExpiration() != null && claims.getExpiration().after(new Date());
            return sameUser && notExpired;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }
}
