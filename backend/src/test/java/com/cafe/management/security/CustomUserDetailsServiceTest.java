package com.cafe.management.security;

import com.cafe.management.entity.Role;
import com.cafe.management.entity.User;
import com.cafe.management.entity.UserStatus;
import com.cafe.management.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Authorization mapping tests: the role stored in MySQL must be converted into the
 * correct Spring Security authority, and a disabled account must be marked disabled.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("STAFF users receive only ROLE_STAFF, never ROLE_ADMIN")
    void staffDoesNotGetAdminAuthority() {
        when(userRepository.findByEmailIgnoreCase("staff@cafe.com")).thenReturn(Optional.of(
                User.builder().id(2L).name("Staff").email("staff@cafe.com").password("hash")
                        .role(Role.STAFF).status(UserStatus.ACTIVE).build()));

        UserDetails details = userDetailsService.loadUserByUsername("staff@cafe.com");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_STAFF")
                .doesNotContain("ROLE_ADMIN");
    }

    @Test
    @DisplayName("ADMIN users receive ROLE_ADMIN and are enabled")
    void adminGetsAdminAuthority() {
        when(userRepository.findByEmailIgnoreCase("admin@cafe.com")).thenReturn(Optional.of(
                User.builder().id(1L).name("Admin").email("admin@cafe.com").password("hash")
                        .role(Role.ADMIN).status(UserStatus.ACTIVE).build()));

        UserDetails details = userDetailsService.loadUserByUsername("admin@cafe.com");

        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("An INACTIVE account is loaded as disabled")
    void inactiveAccountIsDisabled() {
        when(userRepository.findByEmailIgnoreCase("old@cafe.com")).thenReturn(Optional.of(
                User.builder().id(3L).name("Old").email("old@cafe.com").password("hash")
                        .role(Role.STAFF).status(UserStatus.INACTIVE).build()));

        UserDetails details = userDetailsService.loadUserByUsername("old@cafe.com");

        assertThat(details.isEnabled()).isFalse();
    }
}
