package com.amg.digitalitzacio.whatsapp.application;

import java.util.List;

public interface MetaGraphApiClient {

    /** Intercanvia el codi OAuth per un access_token d'usuari */
    String exchangeCodeForToken(String code, String redirectUri);

    /** Obté l'ID d'usuari Meta a partir del token */
    String getUserId(String accessToken);

    /** Llista els WABAs als quals té accés l'usuari */
    List<WabaOption> listWabas(String accessToken);

    /** Llista els números de telèfon d'un WABA concret */
    List<PhoneOption> listPhoneNumbers(String accessToken, String wabaId);

    /** Subscriu el WABA al webhook global de l'app (usa System User Token) */
    void subscribeToWebhook(String systemUserToken, String wabaId);

    /** Revoca els permisos de l'app per a l'usuari Meta */
    void revokePermissions(String accessToken, String metaUserId);

    record WabaOption(String wabaId, String businessName) {}
    record PhoneOption(String phoneNumberId, String displayPhone, String verifiedName) {}
}
