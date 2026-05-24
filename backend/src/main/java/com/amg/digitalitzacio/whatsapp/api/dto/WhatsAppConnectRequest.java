package com.amg.digitalitzacio.whatsapp.api.dto;

/** Connexió manual: l'admin entra el Phone Number ID, token i WABA ID */
public record WhatsAppConnectRequest(
        String phoneNumberId,
        String accessToken,
        String wabaId
) {}
