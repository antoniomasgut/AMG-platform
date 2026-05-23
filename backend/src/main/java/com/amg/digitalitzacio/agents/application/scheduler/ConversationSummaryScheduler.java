package com.amg.digitalitzacio.agents.application.scheduler;

import com.amg.digitalitzacio.agents.application.ConversationService;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationSummaryScheduler {

    private static final int SUMMARY_THRESHOLD = 30;
    private static final String SUMMARY_MODEL = "claude-haiku-4-5-20251001";

    private final ContactRepository contactRepository;
    private final ContactIdentifierRepository contactIdentifierRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final AIProviderRouter aiProviderRouter;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void generatePendingSummaries() {
        var contacts = contactRepository.findAll().stream()
            .filter(c -> c.getTotalMessageCount() != null
                && c.getTotalMessageCount() > SUMMARY_THRESHOLD
                && needsNewSummary(c))
            .toList();

        if (contacts.isEmpty()) return;

        log.info("Generating summaries for {} contacts", contacts.size());
        var provider = aiProviderRouter.forModel(SUMMARY_MODEL);

        for (var contact : contacts) {
            try {
                summarizeContact(contact, provider);
            } catch (Exception e) {
                log.warn("Failed to summarize contact {}: {}", contact.getId(), e.getMessage());
            }
        }
    }

    private boolean needsNewSummary(Contact contact) {
        if (contact.getSummaryUpdatedAt() == null) return true;
        long summarizedCount = contact.getTotalMessageCount() - SUMMARY_THRESHOLD;
        return summarizedCount > 0 && contact.getSummaryUpdatedAt().isBefore(
            java.time.Instant.now().minusSeconds(3600));
    }

    private void summarizeContact(Contact contact, com.amg.digitalitzacio.shared.ai.AIProvider provider) {
        var identifiers = contactIdentifierRepository.findByContactId(contact.getId());
        if (identifiers.isEmpty()) return;

        var allMessages = identifiers.stream()
            .flatMap(ci -> conversationRepository
                .findTop30ByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtDesc(
                    contact.getTenantId(), ci.getIdentifier(), ci.getChannel())
                .stream())
            .sorted(java.util.Comparator.comparing(Conversation::getCreatedAt))
            .toList();

        if (allMessages.isEmpty()) return;

        String transcript = allMessages.stream()
            .map(m -> (m.getRole() == ConversationRole.USER ? "Client" : "Agent")
                + ": " + m.getContent())
            .collect(Collectors.joining("\n"));

        String systemPrompt = """
            Ets un assistent que resumeix converses de suport.
            Fes un resum concís (màxim 200 paraules) de la conversa següent,
            destacant: temes tractats, decisions preses, preferències del client i
            qualsevol informació rellevant per futures interaccions.
            Respon directament amb el resum, sense preàmbuls.
            """;

        String summary = provider.chat(systemPrompt, List.of(), transcript);
        if (summary != null && !summary.isBlank()) {
            conversationService.updateContactSummary(contact.getId(), summary);
            log.debug("Updated summary for contact {}", contact.getId());
        }
    }
}
