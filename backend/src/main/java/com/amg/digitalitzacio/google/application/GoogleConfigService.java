package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleConfigService {

    private static final String CLIENT_ID_KEY = "GOOGLE_OAUTH_CLIENT_ID";
    private static final String CLIENT_SECRET_KEY = "GOOGLE_OAUTH_CLIENT_SECRET";

    private final SystemConfigService systemConfig;

    public String getClientId() {
        var val = systemConfig.get(CLIENT_ID_KEY);
        if (val == null || val.isBlank()) {
            throw new RuntimeException("GOOGLE_OAUTH_CLIENT_ID no configurat");
        }
        return val;
    }

    public String getClientSecret() {
        var val = systemConfig.get(CLIENT_SECRET_KEY);
        if (val == null || val.isBlank()) {
            throw new RuntimeException("GOOGLE_OAUTH_CLIENT_SECRET no configurat");
        }
        return val;
    }
}
