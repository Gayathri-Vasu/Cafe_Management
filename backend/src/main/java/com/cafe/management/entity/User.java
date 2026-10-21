package com.cafe.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Application user (ADMIN or STAFF).
 *
 * <p>The password field always stores a BCrypt hash - the raw password is never
 * persisted and never returned by any API.</p>
 *
 * <p>Besides email + password the account can carry the alternative sign-in
 * credentials used by the login screen:</p>
 * <ul>
 *   <li>{@link #staffCode} + {@link #pinHash} - counter "badge" sign-in.</li>
 *   <li>{@link #phone} - one-time-password sign-in.</li>
 *   <li>{@link #googleSubject} / {@link #appleSubject} - federated identities.</li>
 * </ul>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Family name / surname. Optional. */
    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    /** Short badge number printed on a staff card, unique per user. */
    @Column(name = "staff_code", unique = true, length = 12)
    private String staffCode;

    /** BCrypt hash of the numeric counter PIN. Never returned by any API. */
    @Column(name = "pin_hash")
    private String pinHash;

    /** Mobile number in E.164 format, used for OTP sign-in. */
    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 30)
    private Gender gender;

    /** Date of birth. The profile derives the age from this rather than storing it. */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Town or city the staff member is based in. */
    @Column(name = "place", length = 150)
    private String place;

    @Column(name = "address", length = 500)
    private String address;

    /** Google account id ({@code sub} claim) once the account is linked. */
    @Column(name = "google_subject", unique = true, length = 64)
    private String googleSubject;

    /** Apple account id ({@code sub} claim) once the account is linked. */
    @Column(name = "apple_subject", unique = true, length = 64)
    private String appleSubject;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }
}
