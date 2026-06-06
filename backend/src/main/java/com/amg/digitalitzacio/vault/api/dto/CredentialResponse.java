package com.amg.digitalitzacio.vault.api.dto;

import java.util.UUID;

public record CredentialResponse(UUID id, Boolean isSet, String maskedValue) {

    public static String mask(String value) {
        return "****";
    }
}
