package com.amg.digitalitzacio.vault.api.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String slug;
    private String description;
}
