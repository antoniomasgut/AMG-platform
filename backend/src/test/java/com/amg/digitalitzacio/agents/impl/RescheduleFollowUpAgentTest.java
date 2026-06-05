package com.amg.digitalitzacio.agents.impl;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RescheduleFollowUpAgentTest {

    @Mock
    private TenantChatLinkRepository chatLinkRepository;
    @Mock
    private WhatsAppChannel whatsAppChannel;
    @Mock
    private WhatsAppMetaChannel whatsAppMetaChannel;
    @Mock
    private EmailChannel emailChannel;
    @Mock
    private TelegramBotClient telegramBotClient;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RescheduleFollowUpAgent agent;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Test
    void shouldReturnCorrectMetadata() {
        assertEquals("reschedule-pending", agent.getServiceSlug());
        assertEquals("Seguiment de reprogramació", agent.getDisplayName());
    }

    @Test
    void shouldIgnoreDifferentTaskType() {
        agent.executeTask(tenantId, "SOME_OTHER_TASK", "{}");
        verifyNoInteractions(chatLinkRepository, whatsAppChannel, whatsAppMetaChannel, emailChannel, telegramBotClient);
    }

    @Test
    void shouldSendWhatsAppFollowUp() {
        TenantChatLink chatLink = TenantChatLink.builder()
                .tenantId(tenantId)
                .whatsappPhoneNumber("+34900000000")
                .build();
        when(chatLinkRepository.findByTenantId(tenantId)).thenReturn(Optional.of(chatLink));

        String payload = "{\"identifier\":\"+34612345678\",\"channel\":\"WHATSAPP\",\"name\":\"Maria\"}";
        agent.executeTask(tenantId, "RESCHEDULE_FOLLOWUP", payload);

        verify(whatsAppChannel).sendMessage("+34900000000", "+34612345678", "Hola Maria, encara no hem trobat un nou dia per a la teva cita. Quan et va bé?");
    }

    @Test
    void shouldSendWhatsAppMetaFollowUp() {
        TenantChatLink chatLink = TenantChatLink.builder()
                .tenantId(tenantId)
                .whatsappMetaPhoneNumberId("meta-phone-id")
                .build();
        when(chatLinkRepository.findByTenantId(tenantId)).thenReturn(Optional.of(chatLink));

        String payload = "{\"identifier\":\"+34612345678\",\"channel\":\"WHATSAPP_META\",\"name\":\"Joan\"}";
        agent.executeTask(tenantId, "RESCHEDULE_FOLLOWUP", payload);

        verify(whatsAppMetaChannel).sendMessage("meta-phone-id", "+34612345678", "Hola Joan, encara no hem trobat un nou dia per a la teva cita. Quan et va bé?");
    }

    @Test
    void shouldSendTelegramFollowUp() {
        when(chatLinkRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        String payload = "{\"identifier\":\"123456789\",\"channel\":\"TELEGRAM\",\"name\":\"\"}";
        agent.executeTask(tenantId, "RESCHEDULE_FOLLOWUP", payload);

        verify(telegramBotClient).sendMessage(123456789L, "Hola, encara no hem trobat un nou dia per a la teva cita. Quan et va bé?");
    }

    @Test
    void shouldSendEmailFollowUp() {
        when(chatLinkRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        String payload = "{\"identifier\":\"test@example.com\",\"channel\":\"EMAIL\",\"name\":\"Anna\"}";
        agent.executeTask(tenantId, "RESCHEDULE_FOLLOWUP", payload);

        verify(emailChannel).sendMessage("test@example.com", "Seguiment de reprogramació", "Hola Anna, encara no hem trobat un nou dia per a la teva cita. Quan et va bé?");
    }
}
