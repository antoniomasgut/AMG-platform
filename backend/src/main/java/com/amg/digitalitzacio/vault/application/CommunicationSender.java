package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.domain.CommunicationChannel;

public interface CommunicationSender {
    boolean send(String recipient, String subject, String body);
    CommunicationChannel supportedChannel();
}
