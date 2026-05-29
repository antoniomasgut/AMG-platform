package com.amg.digitalitzacio.leads.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.amg.digitalitzacio.leads.domain.LeadSource;

import java.math.BigDecimal;
import java.util.UUID;

public record LeadRequest(
        @NotBlank @Size(max = 150) String name,
        @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        LeadSource source,
        UUID assignedTo,
        BigDecimal estimatedValue,
        String notes,
        @Size(max = 500) String tags,
        Boolean hasWhatsapp
) {}
