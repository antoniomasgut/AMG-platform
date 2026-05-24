package com.amg.digitalitzacio.whatsapp.application;

public interface MetaWhatsAppClient {

    void sendTextMessage(String phoneNumberId, String accessToken, String toPhone, String text);

    WabaInfo fetchWabaInfo(String accessToken, String wabaId);

    boolean verifyToken(String phoneNumberId, String accessToken);

    record WabaInfo(String wabaId, String phoneNumberId, String displayPhone, String businessName) {}
}
