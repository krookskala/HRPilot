package com.hrpilot.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    java.util.List<User> findByRoleIn(Collection<Role> roles);
    long countByIsActive(boolean isActive);

    @Query("""
        SELECT u FROM User u
        WHERE (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
          AND (:role IS NULL OR u.role = :role)
          AND (:isActive IS NULL OR u.isActive = :isActive)
        """)
    Page<User> search(
        @Param("email") String email,
        @Param("role") Role role,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );
}
