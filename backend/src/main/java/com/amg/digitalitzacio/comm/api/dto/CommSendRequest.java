package com.amg.digitalitzacio.comm.api.dto;

import java.util.Map;

public record CommSendRequest(
        String channel,
        String to,
        String action,
        String sector,
        String language,
        Map<String, String> variables
) {}
