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
    private final ChannelUsageService channelUsageService;

    /** Retorna true si el contacte és nou (creat per primera vegada en aquest canal). */
    @Transactional
    public boolean findOrCreate(UUID tenantId, ConversationChannel channel, String identifier) {
        if (contactIdentifierRepository.findByTenantIdAndChannelAndIdentifier(tenantId, channel, identifier).isPresent()) {
            return false;
        }

        Contact contact = findExistingContactByProfile(tenantId, channel, identifier)
            .orElseGet(() -> contactRepository.save(Contact.builder()
                .tenantId(tenantId)
                .displayName(identifier)
                .build()));

        contactIdentifierRepository.save(ContactIdentifier.builder()
            .contactId(contact.getId())
            .tenantId(tenantId)
            .channel(channel)
            .identifier(identifier)
            .build());
        return true;
    }

    /** Cerca un contact existent per phone (canals WhatsApp) o email (canal EMAIL). */
    private java.util.Optional<Contact> findExistingContactByProfile(
            UUID tenantId, ConversationChannel channel, String identifier) {
        return switch (channel) {
            case WHATSAPP, WHATSAPP_META -> {
                String normalized = normalizePhone(identifier);
                yield contactRepository.findByTenantIdAndPhone(tenantId, normalized);
            }
            case EMAIL -> contactRepository.findByTenantIdAndEmail(tenantId, identifier.toLowerCase());
            default -> java.util.Optional.empty();
        };
    }

    /** Elimina tot el que no sigui dígit i aplica prefix +. */
    private String normalizePhone(String raw) {
        String digits = raw.replaceAll("[^0-9+]", "");
        return digits.isEmpty() ? raw : digits;
    }

    @Transactional(readOnly = true)
    public List<ContactSummaryResponse> listContacts(UUID tenantId) {
        var contacts = contactRepository.findByTenantId(tenantId);
        if (contacts.isEmpty()) return List.of();

        // Pre-càrrega en batch: 3 queries per a tots els contactes en lloc de 3×N×M
        var contactIds = contacts.stream().map(Contact::getId).toList();
        var allIdentifiers = contactIdentifierRepository.findByContactIdIn(contactIds);
        var identifiersByContact = allIdentifiers.stream()
            .collect(java.util.stream.Collectors.groupingBy(ContactIdentifier::getContactId));

        var lastMessages = conversationRepository.findLastMessagePerIdentifierAndChannel(tenantId);
        var lastMsgMap = new java.util.HashMap<String, Conversation>();
        for (var msg : lastMessages) {
            lastMsgMap.put(msg.getCustomerIdentifier() + ":" + msg.getChannel().name(), msg);
        }

        var pendingRaw = conversationRepository.countPendingGroupedByIdentifierAndChannel(tenantId);
        var pendingMap = new java.util.HashMap<String, Long>();
        for (var row : pendingRaw) {
            pendingMap.put(row[0] + ":" + row[1], (Long) row[2]);
        }

        return contacts.stream()
            .map(c -> buildSummaryFromMaps(c,
                identifiersByContact.getOrDefault(c.getId(), List.of()),
                lastMsgMap, pendingMap))
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
        channelUsageService.record(tenantId, lastUsed.getChannel().name());

        conversationService.save(tenantId, lastUsed.getIdentifier(), lastUsed.getChannel(),
            ConversationRole.ASSISTANT, text, false);
    }

    @Transactional
    public void renameContact(UUID tenantId, UUID contactId, String displayName) {
        var contact = assertContactBelongsToTenant(tenantId, contactId);
        contact.setDisplayName(displayName);
        contactRepository.save(contact);
    }

    @Transactional
    public void updateProfile(UUID tenantId, UUID contactId, String displayName, String phone, String email) {
        var contact = assertContactBelongsToTenant(tenantId, contactId);
        if (displayName != null && !displayName.isBlank()) contact.setDisplayName(displayName.trim());
        if (phone != null) contact.setPhone(phone.isBlank() ? null : normalizePhone(phone.trim()));
        if (email != null) contact.setEmail(email.isBlank() ? null : email.trim().toLowerCase());
        contactRepository.save(contact);
    }

    /** Aprova un missatge pending i l'envia al client via el canal original. */
    @Transactional
    public void approveAndSend(UUID tenantId, Long conversationId, String overrideContent) {
        var conversation = conversationRepository.findById(conversationId)
            .filter(c -> c.getTenantId().equals(tenantId) && Boolean.TRUE.equals(c.getPendingApproval()))
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found or not pending"));

        if (overrideContent != null && !overrideContent.isBlank()) {
            conversation.setContent(overrideContent);
        }
        conversation.setPendingApproval(false);
        conversation.setApprovedAt(java.time.Instant.now());
        conversationRepository.save(conversation);

        var chatLink = tenantChatLinkRepository.findByTenantId(tenantId).orElse(null);
        sendViaChannel(chatLink, conversation.getChannel(), conversation.getCustomerIdentifier(), conversation.getContent());
        channelUsageService.record(tenantId, conversation.getChannel().name());
        log.info("Approved and sent conversation {} for tenant {}", conversationId, tenantId);
    }

    private Contact assertContactBelongsToTenant(UUID tenantId, UUID contactId) {
        return contactRepository.findById(contactId)
            .filter(c -> tenantId.equals(c.getTenantId()))
            .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
    }

    private ContactIdentifier findMostRecentlyUsed(UUID tenantId, List<ContactIdentifier> identifiers) {
        var idSet = identifiers.stream().map(ContactIdentifier::getIdentifier).toList();
        var rows = conversationRepository.findMaxCreatedAtPerIdentifier(tenantId, idSet);
        var maxByKey = new java.util.HashMap<String, Instant>();
        for (var row : rows) {
            maxByKey.put(row[0] + ":" + row[1], (Instant) row[2]);
        }
        ContactIdentifier best = identifiers.get(0);
        Instant bestAt = Instant.EPOCH;
        for (var ci : identifiers) {
            Instant last = maxByKey.getOrDefault(ci.getIdentifier() + ":" + ci.getChannel().name(), Instant.EPOCH);
            if (last.isAfter(bestAt)) {
                bestAt = last;
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
                case WIDGET -> log.info("[Widget] Resposta ja mostrada al navegador, no cal reenviar ({})", identifier);
                default -> log.warn("Unsupported channel for manual reply: {}", channel);
            }
        } catch (Exception e) {
            log.error("Error sending manual reply via {}: {}", channel, e.getMessage());
            throw new RuntimeException("Error sending reply: " + e.getMessage(), e);
        }
    }

    private ContactSummaryResponse buildSummaryFromMaps(
            Contact contact,
            List<ContactIdentifier> identifiers,
            java.util.Map<String, Conversation> lastMsgMap,
            java.util.Map<String, Long> pendingMap) {

        String lastContent = null;
        String lastRole = null;
        Instant lastAt = null;
        String lastChannel = null;
        String lastIdentifier = null;
        long pendingCount = 0;

        for (var ci : identifiers) {
            String key = ci.getIdentifier() + ":" + ci.getChannel().name();
            var msg = lastMsgMap.get(key);
            if (msg != null && (lastAt == null || msg.getCreatedAt().isAfter(lastAt))) {
                lastAt = msg.getCreatedAt();
                lastContent = msg.getContent();
                lastRole = msg.getRole().name();
                lastChannel = ci.getChannel().name();
                lastIdentifier = ci.getIdentifier();
            }
            pendingCount += pendingMap.getOrDefault(key, 0L);
        }

        var channelInfos = identifiers.stream()
            .map(ci -> new ContactSummaryResponse.ChannelInfo(ci.getChannel().name(), ci.getIdentifier()))
            .toList();

        int totalMessages = contact.getTotalMessageCount() != null ? contact.getTotalMessageCount() : 0;
        boolean hasSummary = contact.getConversationSummary() != null && !contact.getConversationSummary().isBlank();

        return new ContactSummaryResponse(
            contact.getId(), contact.getDisplayName(), contact.getPhone(), contact.getEmail(),
            channelInfos, lastContent, lastRole, lastAt, lastChannel, lastIdentifier, pendingCount,
            totalMessages, hasSummary);
    }

    @Transactional
    public void clearMemory(UUID tenantId, UUID contactId) {
        contactRepository.findById(contactId).ifPresent(contact -> {
            if (!contact.getTenantId().equals(tenantId))
                throw new IllegalArgumentException("Contact does not belong to tenant");
            contact.setConversationSummary(null);
            contact.setSummaryUpdatedAt(null);
            contactRepository.save(contact);
            log.debug("Cleared memory for contact {} tenant {}", contactId, tenantId);
        });
    }
}
