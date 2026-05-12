package com.amg.digitalitzacio.vault.api.dto;

import lombok.Data;

@Data
public class CreateProfileRequest {
    private String name;
    private String slug;
    private String description;
}
