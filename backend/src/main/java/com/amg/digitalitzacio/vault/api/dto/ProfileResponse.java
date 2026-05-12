package com.amg.digitalitzacio.vault.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProfileResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private boolean isActive;
    private List<PhaseResponse> phases;
}
