package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ContactRepository contactRepository;
    private final ContactIdentifierRepository contactIdentifierRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ContactMessageCountUpdater messageCountUpdater;

    private static final String REDIS_KEY_PATTERN = "conv:%s:%s:%s";

    @Transactional(readOnly = true)
    public List<Conversation> loadHistory(UUID tenantId, String customerIdentifier, ConversationChannel channel) {
        return loadCustomerContext(tenantId, customerIdentifier, channel).recentMessages();
    }

    @Transactional(readOnly = true)
    public CustomerContext loadCustomerContext(UUID tenantId, String customerIdentifier, ConversationChannel channel) {
        var recent = loadRecentFromDatabase(tenantId, customerIdentifier, channel);
        long total = conversationRepository.countByTenantIdAndCustomerIdentifierAndChannel(
            tenantId, customerIdentifier, channel);

        String summary = contactIdentifierRepository
            .findByTenantIdAndChannelAndIdentifier(tenantId, channel, customerIdentifier)
            .map(ci -> contactRepository.findById(ci.getContactId()).orElse(null))
            .map(Contact::getConversationSummary)
            .orElse(null);

        return new CustomerContext(recent, summary, total);
    }

    @Transactional
    public void save(UUID tenantId, String customerIdentifier, ConversationChannel channel,
                     ConversationRole role, String content, boolean pendingApproval) {
        Conversation conversation = Conversation.builder()
            .tenantId(tenantId)
            .customerIdentifier(customerIdentifier)
            .channel(channel)
            .role(role)
            .content(content)
            .pendingApproval(pendingApproval)
            .build();

        conversationRepository.save(conversation);
        messageCountUpdater.increment(tenantId, customerIdentifier, channel);
        invalidateCache(tenantId, customerIdentifier, channel);

        log.debug("Saved conversation: tenant={}, customer={}, channel={}, role={}, pending={}",
            tenantId, customerIdentifier, channel, role, pendingApproval);
    }

    @Transactional
    public void updateContactSummary(UUID contactId, String summary) {
        contactRepository.findById(contactId).ifPresent(contact -> {
            contact.setConversationSummary(summary);
            contact.setSummaryUpdatedAt(Instant.now());
            contactRepository.save(contact);
        });
    }

    @Transactional(readOnly = true)
    public List<Conversation> loadHistoryAcrossChannels(UUID tenantId, String customerIdentifier,
            java.util.Collection<ConversationChannel> channels) {
        var conversations = conversationRepository
            .findRecentByTenantIdAndCustomerIdentifierAndChannels(tenantId, customerIdentifier, channels);
        Collections.reverse(conversations);
        return conversations;
    }

    private List<Conversation> loadRecentFromDatabase(UUID tenantId, String customerIdentifier, ConversationChannel channel) {
        var conversations = conversationRepository
            .findTop30ByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtDesc(
                tenantId, customerIdentifier, channel);
        Collections.reverse(conversations);
        return conversations;
    }

    /**
     * Finalitza un esborrany pendent amb el text realment enviat: actualitza el darrer
     * missatge ASSISTANT pendent (contingut + treu el flag pendingApproval). Si no en
     * troba cap (p. ex. resposta manual sense esborrany previ), desa el text com a
     * missatge ASSISTANT confirmat. Així el panell central mostra sempre el que es va enviar.
     */
    @Transactional
    public void finalizeSentReply(UUID tenantId, String customerIdentifier,
                                  ConversationChannel channel, String sentText) {
        var pending = conversationRepository
            .findTop1ByTenantIdAndCustomerIdentifierAndChannelAndRoleAndPendingApprovalTrueOrderByCreatedAtDesc(
                tenantId, customerIdentifier, channel, ConversationRole.ASSISTANT);
        if (pending.isPresent()) {
            Conversation c = pending.get();
            c.setContent(sentText);
            c.setPendingApproval(false);
            conversationRepository.save(c);
        } else {
            save(tenantId, customerIdentifier, channel, ConversationRole.ASSISTANT, sentText, false);
        }
        invalidateCache(tenantId, customerIdentifier, channel);
    }

    private void invalidateCache(UUID tenantId, String customerIdentifier, ConversationChannel channel) {
        String redisKey = String.format(REDIS_KEY_PATTERN, tenantId, channel.name(), customerIdentifier);
        try {
            stringRedisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Redis cache invalidation failed for key: {}", redisKey, e);
        }
    }
}
