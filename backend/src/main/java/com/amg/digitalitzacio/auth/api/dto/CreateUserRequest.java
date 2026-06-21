package com.amg.digitalitzacio.auth.api.dto;

import com.amg.digitalitzacio.shared.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 80) String position,
        @NotNull Role role,
        UUID tenantId
) {}
