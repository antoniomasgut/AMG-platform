package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.shared.config.TestAsyncConfig;
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
@Import({TestRedisConfig.class, TestAsyncConfig.class})
@Transactional
class ConversationServiceTest {

    @Autowired private ConversationService conversationService;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ContactRepository contactRepository;
    @Autowired private ContactIdentifierRepository contactIdentifierRepository;

    private UUID tenantId;
    private Contact contact;
    private static final String CUSTOMER = "test-customer-1";
    private static final ConversationChannel CHANNEL = ConversationChannel.TELEGRAM;

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
                .channel(CHANNEL)
                .identifier(CUSTOMER)
                .build());
    }

    @Test
    void loadHistory_returnsEmpty_whenNoMessages() {
        var history = conversationService.loadHistory(tenantId, CUSTOMER, CHANNEL);
        assertThat(history, is(empty()));
    }

    @Test
    void loadHistory_returnsRecentMessages() {
        var msg1 = conversationRepository.save(Conversation.builder()
                .tenantId(tenantId).customerIdentifier(CUSTOMER).channel(CHANNEL)
                .role(ConversationRole.USER).content("Hola").build());
        var msg2 = conversationRepository.save(Conversation.builder()
                .tenantId(tenantId).customerIdentifier(CUSTOMER).channel(CHANNEL)
                .role(ConversationRole.ASSISTANT).content("Bones").build());

        var history = conversationService.loadHistory(tenantId, CUSTOMER, CHANNEL);
        assertThat(history, hasSize(2));
        assertThat(history.getFirst().getContent(), is("Hola"));
        assertThat(history.get(1).getContent(), is("Bones"));
    }

    @Test
    void loadHistory_returnsAtMost30Messages() {
        for (int i = 0; i < 35; i++) {
            conversationRepository.save(Conversation.builder()
                    .tenantId(tenantId).customerIdentifier(CUSTOMER).channel(CHANNEL)
                    .role(ConversationRole.USER).content("Msg " + i).build());
        }

        var history = conversationService.loadHistory(tenantId, CUSTOMER, CHANNEL);
        assertThat(history, hasSize(30));
    }

    @Test
    void loadCustomerContext_containsSummary_whenContactHasOne() {
        contact.setConversationSummary("Resum de converses anteriors");
        contactRepository.save(contact);

        var ctx = conversationService.loadCustomerContext(tenantId, CUSTOMER, CHANNEL);
        assertThat(ctx.summary(), is("Resum de converses anteriors"));
        assertThat(ctx.totalMessageCount(), is(0L));
    }

    @Test
    void loadCustomerContext_returnsNullSummary_whenNoConversations() {
        var ctx = conversationService.loadCustomerContext(tenantId, CUSTOMER, CHANNEL);
        assertThat(ctx.summary(), is(nullValue()));
        assertThat(ctx.totalMessageCount(), is(0L));
    }

    @Test
    void save_incrementsMessageCount() {
        conversationService.save(tenantId, CUSTOMER, CHANNEL, ConversationRole.USER, "Hola", false);
        conversationService.save(tenantId, CUSTOMER, CHANNEL, ConversationRole.ASSISTANT, "Bones", false);

        var updated = contactRepository.findById(contact.getId());
        assertThat(updated.isPresent(), is(true));
        assertThat(updated.get().getTotalMessageCount(), is(2));
    }

    @Test
    void save_createsPendingMessage() {
        conversationService.save(tenantId, CUSTOMER, CHANNEL, ConversationRole.USER, "Missatge pendent", true);

        var msgs = conversationRepository
                .findTop30ByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtDesc(
                        tenantId, CUSTOMER, CHANNEL);

        assertThat(msgs, hasSize(1));
        assertThat(msgs.getFirst().getPendingApproval(), is(true));
        assertThat(msgs.getFirst().getContent(), is("Missatge pendent"));
    }

    @Test
    void updateContactSummary_savesSummary() {
        var now = Instant.now();
        conversationService.updateContactSummary(contact.getId(), "Nou resum de conversa");

        var updated = contactRepository.findById(contact.getId());
        assertThat(updated.isPresent(), is(true));
        assertThat(updated.get().getConversationSummary(), is("Nou resum de conversa"));
        assertThat(updated.get().getSummaryUpdatedAt(), is(notNullValue()));
        assertThat(updated.get().getSummaryUpdatedAt().isAfter(now) || updated.get().getSummaryUpdatedAt().equals(now), is(true));
    }

    @Test
    void loadHistory_returnsEmptyForUnknownCustomer() {
        var history = conversationService.loadHistory(tenantId, "unknown-customer", CHANNEL);
        assertThat(history, is(empty()));
    }

    @Test
    void loadCustomerContext_countMatchesTotalMessages() {
        for (int i = 0; i < 5; i++) {
            conversationRepository.save(Conversation.builder()
                    .tenantId(tenantId).customerIdentifier(CUSTOMER).channel(CHANNEL)
                    .role(ConversationRole.USER).content("Msg " + i).build());
        }

        var ctx = conversationService.loadCustomerContext(tenantId, CUSTOMER, CHANNEL);
        assertThat(ctx.totalMessageCount(), is(5L));
        assertThat(ctx.recentMessages(), hasSize(5));
    }
}
