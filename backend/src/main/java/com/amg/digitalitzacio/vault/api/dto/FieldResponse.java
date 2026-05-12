package com.amg.digitalitzacio.vault.api.dto;

import com.amg.digitalitzacio.vault.domain.FieldType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FieldResponse {
    private UUID id;
    private String key;
    private String label;
    private FieldType type;
    private boolean isRequired;
    private String placeholder;
    private String validationRegex;
    private Integer sortOrder;
}
