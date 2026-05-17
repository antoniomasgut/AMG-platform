package com.amg.digitalitzacio.leads.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.amg.digitalitzacio.leads.domain.ActivityType;

import java.time.Instant;

public record ActivityRequest(
        @NotNull ActivityType type,
        @NotBlank String description,
        Instant dueDate
) {}
