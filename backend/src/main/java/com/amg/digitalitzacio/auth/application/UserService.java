package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.auth.api.dto.*;
import com.amg.digitalitzacio.auth.domain.*;
import com.amg.digitalitzacio.shared.security.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(@Valid CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email ja registrat");
        }

        if (request.role() == Role.CLIENT && request.tenantId() == null) {
            throw new IllegalArgumentException("El rol CLIENT requereix un tenantId");
        }

        if (request.role() == Role.SUPER_ADMIN && request.tenantId() != null) {
            // SUPER_ADMIN no te tenant
        }

        if (request.tenantId() != null && !tenantRepository.existsById(request.tenantId())) {
            throw new IllegalArgumentException("Tenant no trobat");
        }

        var user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(request.role())
                .tenantId(request.role() == Role.CLIENT ? request.tenantId() : null)
                .isActive(true)
                .isBlocked(false)
                .failedAttempts(0)
                .build();

        user = userRepository.save(user);
        return toResponse(user);
    }

    public Page<UserResponse> listUsers(Pageable pageable, Role role, UUID tenantId, String search) {
        // Per simplicitat, retornem tots amb paginacio basica
        // En una versio real, afegir Specifications per filtrar
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    public UserResponse getUser(UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, @Valid UpdateUserRequest request) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));

        if (request.email() != null) {
            if (!request.email().equals(user.getEmail()) && userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email ja registrat");
            }
            user.setEmail(request.email());
        }
        if (request.name() != null) user.setName(request.name());
        if (request.role() != null) user.setRole(request.role());
        if (request.tenantId() != null) user.setTenantId(request.tenantId());
        if (request.isActive() != null) user.setIsActive(request.isActive());

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public void deactivateUser(UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void unlockUser(UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));
        user.resetFailedAttempts();
        userRepository.save(user);
    }

    private UserResponse toResponse(User user) {
        UserResponse.TenantRef tenantRef = null;
        if (user.getTenantId() != null) {
            var tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
            if (tenant != null) {
                tenantRef = new UserResponse.TenantRef(tenant.getId(), tenant.getName());
            }
        }
        return new UserResponse(
                user.getId(), user.getEmail(), user.getName(), user.getRole(),
                tenantRef, user.getIsActive(), user.getIsBlocked(),
                user.getLastLoginAt(), user.getCreatedAt());
    }
}
