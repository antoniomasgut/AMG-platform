package com.amg.digitalitzacio.engine.api.dto;

import java.util.Map;

public record LandingDefaultsResponse(
    String businessName,
    String phone,
    String email,
    String address,
    String city,
    String sector,
    String whatsappNumber,
    Map<String, String> heroSuggestions,
    Map<String, String> contactSuggestions
) {}
