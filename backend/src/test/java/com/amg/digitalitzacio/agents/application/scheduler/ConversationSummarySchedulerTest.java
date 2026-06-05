package com.amg.digitalitzacio.agents.application.scheduler;

import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class ConversationSummarySchedulerTest {

    @Autowired private ConversationSummaryScheduler scheduler;
    @Autowired private ContactRepository contactRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ContactIdentifierRepository contactIdentifierRepository;

    private UUID tenantId;
    private Contact contact;

    @BeforeEach
    void setUp() {
        conversationRepository.deleteAll();
        contactRepository.deleteAll();
        contactIdentifierRepository.deleteAll();

        tenantId = UUID.randomUUID();
        contact = contactRepository.save(Contact.builder()
                .tenantId(tenantId).displayName("Test Client")
                .totalMessageCount(0).build());

        contactIdentifierRepository.save(ContactIdentifier.builder()
                .tenantId(tenantId)
                .contactId(contact.getId())
                .channel(ConversationChannel.TELEGRAM)
                .identifier("test-customer")
                .build());
    }

    @Test
    void scheduler_skipsContacts_belowThreshold() {
        contact.setTotalMessageCount(10);
        contactRepository.save(contact);

        scheduler.generatePendingSummaries();

        var updated = contactRepository.findById(contact.getId());
        assertThat(updated.isPresent(), is(true));
        assertThat(updated.get().getConversationSummary(), is(nullValue()));
    }

    @Test
    void scheduler_skipsRecentlySummarized() {
        contact.setTotalMessageCount(35);
        contact.setSummaryUpdatedAt(Instant.now());
        contactRepository.save(contact);

        scheduler.generatePendingSummaries();

        var updated = contactRepository.findById(contact.getId());
        assertThat(updated.isPresent(), is(true));
        assertThat(updated.get().getConversationSummary(), is(nullValue()));
    }

    @Test
    void scheduler_handlesContacts_withoutIdentifiers() {
        contactIdentifierRepository.deleteAll();

        contact.setTotalMessageCount(10);
        contactRepository.save(contact);

        scheduler.generatePendingSummaries();
    }
}
