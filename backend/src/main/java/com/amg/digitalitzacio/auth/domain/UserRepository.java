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
            AND (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<User> findAllFiltered(@Param("role") Role role,
                               @Param("tenantId") UUID tenantId,
                               @Param("search") String search,
                               Pageable pageable);
}
