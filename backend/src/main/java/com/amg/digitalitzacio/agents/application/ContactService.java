package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.api.dto.ContactSummaryResponse;
import com.amg.digitalitzacio.agents.api.dto.ConversationResponse;
import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final ContactIdentifierRepository contactIdentifierRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final TenantChatLinkRepository tenantChatLinkRepository;
    private final TelegramBotClient telegramBotClient;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final EmailChannel emailChannel;

    @Transactional
    public void findOrCreate(UUID tenantId, ConversationChannel channel, String identifier) {
        contactIdentifierRepository.findByTenantIdAndChannelAndIdentifier(tenantId, channel, identifier)
            .orElseGet(() -> {
                var contact = contactRepository.save(Contact.builder()
                    .tenantId(tenantId)
                    .displayName(identifier)
                    .build());
                return contactIdentifierRepository.save(ContactIdentifier.builder()
                    .contactId(contact.getId())
                    .tenantId(tenantId)
                    .channel(channel)
                    .identifier(identifier)
                    .build());
            });
    }

    @Transactional(readOnly = true)
    public List<ContactSummaryResponse> listContacts(UUID tenantId) {
        return contactRepository.findByTenantId(tenantId).stream()
            .map(c -> buildSummary(tenantId, c))
            .filter(s -> s.lastMessageAt() != null)
            .sorted(Comparator.comparing(ContactSummaryResponse::lastMessageAt).reversed())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getThread(UUID tenantId, UUID contactId) {
        assertContactBelongsToTenant(tenantId, contactId);

        var identifiers = contactIdentifierRepository.findByContactId(contactId);
        var messages = new ArrayList<com.amg.digitalitzacio.agents.domain.Conversation>();
        for (var ci : identifiers) {
            messages.addAll(conversationRepository
                .findByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtAsc(
                    tenantId, ci.getIdentifier(), ci.getChannel()));
        }

        return messages.stream()
            .sorted(Comparator.comparing(com.amg.digitalitzacio.agents.domain.Conversation::getCreatedAt))
            .map(c -> new ConversationResponse(
                c.getId(), c.getCustomerIdentifier(), c.getChannel(),
                c.getRole(), c.getContent(), c.getPendingApproval(), c.getCreatedAt()))
            .toList();
    }

    @Transactional
    public void sendReply(UUID tenantId, UUID contactId, String text) {
        assertContactBelongsToTenant(tenantId, contactId);

        var identifiers = contactIdentifierRepository.findByContactId(contactId);
        if (identifiers.isEmpty()) throw new IllegalStateException("No identifiers for contact");

        var lastUsed = findMostRecentlyUsed(tenantId, identifiers);

        var chatLink = tenantChatLinkRepository.findByTenantId(tenantId).orElse(null);
        sendViaChannel(chatLink, lastUsed.getChannel(), lastUsed.getIdentifier(), text);

        conversationService.save(tenantId, lastUsed.getIdentifier(), lastUsed.getChannel(),
            ConversationRole.ASSISTANT, text, false);
    }

    @Transactional
    public void renameContact(UUID tenantId, UUID contactId, String displayName) {
        var contact = assertContactBelongsToTenant(tenantId, contactId);
        contact.setDisplayName(displayName);
        contactRepository.save(contact);
    }

    private Contact assertContactBelongsToTenant(UUID tenantId, UUID contactId) {
        return contactRepository.findById(contactId)
            .filter(c -> tenantId.equals(c.getTenantId()))
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
    }

    private ContactIdentifier findMostRecentlyUsed(UUID tenantId, List<ContactIdentifier> identifiers) {
        ContactIdentifier best = identifiers.get(0);
        Instant bestAt = Instant.EPOCH;
        for (var ci : identifiers) {
            var last = conversationRepository
                .findTop1ByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtDesc(
                    tenantId, ci.getIdentifier(), ci.getChannel());
            if (last.isPresent() && last.get().getCreatedAt().isAfter(bestAt)) {
                bestAt = last.get().getCreatedAt();
                best = ci;
            }
        }
        return best;
    }

    private void sendViaChannel(TenantChatLink chatLink, ConversationChannel channel,
                                String identifier, String text) {
        try {
            switch (channel) {
                case TELEGRAM -> telegramBotClient.sendMessage(Long.parseLong(identifier), text);
                case WHATSAPP -> {
                    String from = chatLink != null ? chatLink.getWhatsappPhoneNumber() : "";
                    whatsAppChannel.sendMessage(from != null ? from : "", identifier, text);
                }
                case WHATSAPP_META -> {
                    String phoneNumberId = chatLink != null ? chatLink.getWhatsappMetaPhoneNumberId() : "";
                    whatsAppMetaChannel.sendMessage(phoneNumberId != null ? phoneNumberId : "", identifier, text);
                }
                case EMAIL -> emailChannel.sendMessage(identifier, "Resposta de l'equip", text);
                default -> log.warn("Unsupported channel for manual reply: {}", channel);
            }
        } catch (Exception e) {
            log.error("Error sending manual reply via {}: {}", channel, e.getMessage());
            throw new RuntimeException("Error sending reply: " + e.getMessage(), e);
        }
    }

    private ContactSummaryResponse buildSummary(UUID tenantId, Contact contact) {
        var identifiers = contactIdentifierRepository.findByContactId(contact.getId());

        String lastContent = null;
        String lastRole = null;
        Instant lastAt = null;
        String lastChannel = null;
        String lastIdentifier = null;
        long pendingCount = 0;

        for (var ci : identifiers) {
            var lastMsg = conversationRepository
                .findTop1ByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtDesc(
                    tenantId, ci.getIdentifier(), ci.getChannel());
            if (lastMsg.isPresent()) {
                var msg = lastMsg.get();
                if (lastAt == null || msg.getCreatedAt().isAfter(lastAt)) {
                    lastAt = msg.getCreatedAt();
                    lastContent = msg.getContent();
                    lastRole = msg.getRole().name();
                    lastChannel = ci.getChannel().name();
                    lastIdentifier = ci.getIdentifier();
                }
            }
            pendingCount += conversationRepository
                .countByTenantIdAndCustomerIdentifierAndChannelAndPendingApprovalTrue(
                    tenantId, ci.getIdentifier(), ci.getChannel());
        }

        var channelInfos = identifiers.stream()
            .map(ci -> new ContactSummaryResponse.ChannelInfo(ci.getChannel().name(), ci.getIdentifier()))
            .toList();

        return new ContactSummaryResponse(
            contact.getId(), contact.getDisplayName(), channelInfos,
            lastContent, lastRole, lastAt, lastChannel, lastIdentifier, pendingCount);
    }
}
