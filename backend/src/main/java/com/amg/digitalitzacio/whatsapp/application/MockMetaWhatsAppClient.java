package com.amg.digitalitzacio.whatsapp.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MockMetaWhatsAppClient implements MetaWhatsAppClient {

    @Override
    public void sendTextMessage(String phoneNumberId, String accessToken, String toPhone, String text) {
        log.info("[MOCK-META] Sending to {} via phoneNumberId={}: {}", toPhone, phoneNumberId, text);
    }

    @Override
    public WabaInfo fetchWabaInfo(String accessToken, String wabaId) {
        return new WabaInfo(
                wabaId != null ? wabaId : "MOCK_WABA_ID",
                "123456789012345",
                "+34612345678",
                "Negoci Demo"
        );
    }

    @Override
    public boolean verifyToken(String phoneNumberId, String accessToken) {
        return accessToken != null && !accessToken.isBlank()
                && phoneNumberId != null && !phoneNumberId.isBlank();
    }
}
