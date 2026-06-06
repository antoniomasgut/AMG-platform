package com.amg.digitalitzacio.shared.mail;

import java.io.InputStream;

public interface MailProvider {
    void send(String to, String subject, String body, String attachmentName, InputStream attachmentData, String attachmentMimeType);
    String getProviderName();
}
