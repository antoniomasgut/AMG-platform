package com.amg.digitalitzacio.vault.api.dto;

import java.util.UUID;

public record CredentialResponse(UUID id, Boolean isSet, String maskedValue) {
    private static final int MASK_LENGTH = 4;

    public static String mask(String value) {
        if (value == null || value.length() <= MASK_LENGTH) return "***";
        return "***" + value.substring(value.length() - MASK_LENGTH);
    }
}
