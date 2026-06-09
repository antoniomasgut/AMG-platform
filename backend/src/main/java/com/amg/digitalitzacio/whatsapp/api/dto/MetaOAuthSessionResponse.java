package com.amg.digitalitzacio.whatsapp.api.dto;

import java.util.List;

public record MetaOAuthSessionResponse(
    String sessionToken,
    List<WabaChoice> wabas
) {
    public record WabaChoice(String wabaId, String businessName, List<PhoneChoice> phones) {}
    public record PhoneChoice(String phoneNumberId, String displayPhone, String verifiedName) {}
}
