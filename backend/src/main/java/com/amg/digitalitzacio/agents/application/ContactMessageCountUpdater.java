package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class ContactMessageCountUpdater {

    private final ContactIdentifierRepository contactIdentifierRepository;
    private final ContactRepository contactRepository;

    @Async
    @Transactional
    void increment(UUID tenantId, String customerIdentifier, ConversationChannel channel) {
        try {
            contactIdentifierRepository
                .findByTenantIdAndChannelAndIdentifier(tenantId, channel, customerIdentifier)
                .ifPresent(ci -> contactRepository.findById(ci.getContactId()).ifPresent(contact -> {
                    contact.setTotalMessageCount(contact.getTotalMessageCount() + 1);
                    contactRepository.save(contact);
                }));
        } catch (Exception e) {
            log.warn("Could not increment message count for tenant={}, customer={}: {}",
                tenantId, customerIdentifier, e.getMessage());
        }
    }
}
