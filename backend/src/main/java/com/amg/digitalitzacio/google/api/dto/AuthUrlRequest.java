package com.amg.digitalitzacio.google.api.dto;

import java.util.List;

public record AuthUrlRequest(
    List<String> modules,
    String redirectUri
) {}
