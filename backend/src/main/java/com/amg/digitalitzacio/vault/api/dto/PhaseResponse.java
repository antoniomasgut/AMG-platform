package com.amg.digitalitzacio.vault.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PhaseResponse {
    private UUID id;
    private String name;
    private String description;
    private Integer sortOrder;
    private List<ServiceResponse> services;
}
