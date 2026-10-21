package com.cafe.management.repository;

import com.cafe.management.entity.Role;
import com.cafe.management.entity.User;
import com.cafe.management.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByStaffCode(String staffCode);

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    Optional<User> findByGoogleSubject(String googleSubject);

    Optional<User> findByAppleSubject(String appleSubject);

    List<User> findByRole(Role role);

    List<User> findByRoleAndStatus(Role role, UserStatus status);

    /** Used to decide whether a demo STAFF account still needs to be seeded. */
    long countByRole(Role role);

    /**
     * Lists users (ADMIN and STAFF) filtered by name/email (case-insensitive).
     * Pass {@code null} to return every user.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:search IS NULL
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY u.name ASC
            """)
    List<User> search(@Param("search") String search);
}
