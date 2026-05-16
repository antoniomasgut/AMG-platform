package com.amg.digitalitzacio.auth.domain;

import com.amg.digitalitzacio.shared.security.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
            AND (:tenantId IS NULL OR u.tenantId = :tenantId)
            AND (:searchPattern IS NULL OR LOWER(u.name) LIKE :searchPattern
                 OR LOWER(u.email) LIKE :searchPattern)
            """)
    Page<User> findAllFiltered(@Param("role") Role role,
                               @Param("tenantId") UUID tenantId,
                               @Param("searchPattern") String searchPattern,
                               Pageable pageable);
}
