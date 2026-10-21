package com.cafe.management.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies JWT issuing / validation and that an expired or tampered token is rejected.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-for-hs256";

    @Test
    @DisplayName("A generated token can be parsed back and validated")
    void generatesAndValidatesToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserDetails user = User.withUsername("admin@cafe.com").password("x").authorities("ROLE_ADMIN").build();

        String token = jwtService.generateToken("admin@cafe.com", "ADMIN");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@cafe.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("A token belonging to another user is not valid for this user")
    void rejectsTokenForDifferentUser() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserDetails other = User.withUsername("staff@cafe.com").password("x").authorities("ROLE_STAFF").build();

        String token = jwtService.generateToken("admin@cafe.com", "ADMIN");

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    @DisplayName("An already expired token is rejected")
    void rejectsExpiredToken() {
        JwtService jwtService = new JwtService(SECRET, -1_000);
        UserDetails user = User.withUsername("admin@cafe.com").password("x").authorities("ROLE_ADMIN").build();

        String token = jwtService.generateToken("admin@cafe.com", "ADMIN");

        assertThat(jwtService.isTokenValid(token, user)).isFalse();
    }

    @Test
    @DisplayName("A tampered (wrongly signed) token is rejected")
    void rejectsTamperedToken() {
        JwtService issuer = new JwtService(SECRET, 60_000);
        JwtService validator = new JwtService("another-secret-key-that-is-also-long-enough-32chars", 60_000);
        UserDetails user = User.withUsername("admin@cafe.com").password("x").authorities("ROLE_ADMIN").build();

        String token = issuer.generateToken("admin@cafe.com", "ADMIN");

        assertThat(validator.isTokenValid(token, user)).isFalse();
    }
}
