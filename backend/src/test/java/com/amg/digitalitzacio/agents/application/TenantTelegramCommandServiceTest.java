package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.booking.domain.BookingToken;
import com.amg.digitalitzacio.booking.domain.BookingTokenRepository;
import com.amg.digitalitzacio.documents.builder.domain.DocumentStatus;
import com.amg.digitalitzacio.documents.builder.domain.GeneratedDocument;
import com.amg.digitalitzacio.documents.builder.domain.GeneratedDocumentRepository;
import com.amg.digitalitzacio.google.application.GoogleBusinessReviewSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantTelegramCommandServiceTest {

    @Mock TenantChatLinkRepository chatLinkRepository;
    @Mock TenantRepository tenantRepository;
    @Mock ConversationRepository conversationRepository;
    @Mock ContactRepository contactRepository;
    @Mock BookingTokenRepository bookingTokenRepository;
    @Mock GeneratedDocumentRepository documentRepository;
    @Mock GoogleBusinessReviewSyncService reviewSyncService;
    @Mock NexeServiceConfigService nexeServiceConfigService;
    @Mock WhatsAppChannel whatsAppChannel;
    @Mock WhatsAppMetaChannel whatsAppMetaChannel;
    @Mock EmailChannel emailChannel;
    @Mock ObjectMapper objectMapper;

    @InjectMocks TenantTelegramCommandService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final ZoneId TZ = ZoneId.of("Europe/Madrid");

    private Tenant tenant(String phases) {
        return Tenant.builder().id(TENANT_ID).name("Test").slug("test")
                .contractedPhases(phases).build();
    }

    private TenantChatLink chatLink(AgentMode mode) {
        var link = new TenantChatLink();
        link.setTenantId(TENANT_ID);
        link.setIsActive(true);
        link.setAgentMode(mode);
        return link;
    }

    // ── /ajuda ────────────────────────────────────────────────────────────────

    @Test
    void ajuda_noActivePhases_returnsNoServiceMessage() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("")));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(nexeServiceConfigService.get(TENANT_ID, "SOCIAL_PUBLISHER")).thenReturn(Optional.empty());

        String result = service.handleAjuda(TENANT_ID);

        assertThat(result).contains("Encara no tens cap servei actiu");
    }

    @Test
    void ajuda_withF1_showsAgentCommands() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F1")));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(chatLink(AgentMode.AUTO)));
        when(nexeServiceConfigService.get(TENANT_ID, "SOCIAL_PUBLISHER")).thenReturn(Optional.empty());

        String result = service.handleAjuda(TENANT_ID);

        assertThat(result).contains("/mode auto");
        assertThat(result).contains("/stats");
        assertThat(result).doesNotContain("/agenda");
    }

    @Test
    void ajuda_withF2_showsAgendaCommands() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F2")));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(nexeServiceConfigService.get(TENANT_ID, "SOCIAL_PUBLISHER")).thenReturn(Optional.empty());

        String result = service.handleAjuda(TENANT_ID);

        assertThat(result).contains("/agenda");
        assertThat(result).contains("/absencia");
        assertThat(result).doesNotContain("/pressupost");
    }

    @Test
    void ajuda_withF3_showsDocumentCommands() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F3")));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(nexeServiceConfigService.get(TENANT_ID, "SOCIAL_PUBLISHER")).thenReturn(Optional.empty());

        String result = service.handleAjuda(TENANT_ID);

        assertThat(result).contains("/pressupost");
        assertThat(result).contains("/pendents");
    }

    // ── /mode ─────────────────────────────────────────────────────────────────

    @Test
    void mode_f1NotActive_returnsGatedMessage() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F2")));

        String result = service.handleMode(TENANT_ID, "/mode auto");

        assertThat(result).contains("no està activat");
        verifyNoInteractions(chatLinkRepository);
    }

    @Test
    void mode_auto_savesAutoMode() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F1")));
        var link = chatLink(AgentMode.MANUAL);
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(link));
        when(chatLinkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String result = service.handleMode(TENANT_ID, "/mode auto");

        assertThat(result).contains("Auto");
        verify(chatLinkRepository).save(argThat(l -> ((TenantChatLink) l).getAgentMode() == AgentMode.AUTO));
    }

    @Test
    void mode_manual_savesManualMode() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F1")));
        var link = chatLink(AgentMode.AUTO);
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(link));
        when(chatLinkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String result = service.handleMode(TENANT_ID, "/mode manual");

        assertThat(result).contains("Manual");
        verify(chatLinkRepository).save(argThat(l -> ((TenantChatLink) l).getAgentMode() == AgentMode.MANUAL));
    }

    @Test
    void mode_invalidArg_showsCurrentModeAndOptions() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F1")));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(chatLink(AgentMode.HYBRID)));

        String result = service.handleMode(TENANT_ID, "/mode");

        assertThat(result).contains("Mode actual");
        assertThat(result).contains("/mode auto");
        verify(chatLinkRepository, never()).save(any());
    }

    // ── /stats ────────────────────────────────────────────────────────────────

    @Test
    void stats_f1NotActive_returnsGatedMessage() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F2")));

        String result = service.handleStats(TENANT_ID);

        assertThat(result).contains("no està activat");
    }

    @Test
    void stats_f1Active_returnsFormattedStats() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F1")));
        when(conversationRepository.countByTenantIdAndCreatedAtAfter(eq(TENANT_ID), any())).thenReturn(3L, 10L);
        when(conversationRepository.countByTenantIdAndPendingApprovalTrue(TENANT_ID)).thenReturn(0L);
        when(contactRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(chatLink(AgentMode.AUTO)));

        String result = service.handleStats(TENANT_ID);

        assertThat(result).contains("Activitat");
        assertThat(result).contains("Converses noves");
        assertThat(result).contains("Mode agent");
    }

    // ── /pendents ─────────────────────────────────────────────────────────────

    @Test
    void pendents_f3NotActive_returnsGatedMessage() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F1")));

        String result = service.handlePendents(TENANT_ID);

        assertThat(result).contains("no està activada");
        verifyNoInteractions(documentRepository);
    }

    @Test
    void pendents_f3Active_noDocuments_returnsOkMessage() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F3")));
        when(documentRepository.findByTenantIdAndStatus(TENANT_ID, DocumentStatus.SENT)).thenReturn(List.of());

        String result = service.handlePendents(TENANT_ID);

        assertThat(result).contains("Cap document pendent");
    }

    @Test
    void pendents_f3Active_withDocuments_listsNumbers() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F3")));

        GeneratedDocument doc = mock(GeneratedDocument.class);
        when(doc.getNumber()).thenReturn("PRE-2026-001");
        when(doc.getCustomerData()).thenReturn("{\"name\":\"Joan Ferrer\"}");
        when(doc.getCreatedAt()).thenReturn(Instant.now().minusSeconds(86400));

        when(documentRepository.findByTenantIdAndStatus(TENANT_ID, DocumentStatus.SENT))
                .thenReturn(List.of(doc));

        // objectMapper.readValue needs to work — use a real ObjectMapper for this
        // but since it's mocked, we test partial behavior
        String result = service.handlePendents(TENANT_ID);

        assertThat(result).contains("PRE-2026-001");
    }

    // ── /cancel ───────────────────────────────────────────────────────────────

    @Test
    void cancel_f2NotActive_returnsGatedMessage() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F1")));

        String result = service.handleCancel(TENANT_ID, "/cancel 10:30");

        assertThat(result).contains("no està activada");
        verifyNoInteractions(bookingTokenRepository);
    }

    @Test
    void cancel_noArg_returnsFormatHelp() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F2")));

        String result = service.handleCancel(TENANT_ID, "/cancel");

        assertThat(result).contains("Indica l'hora");
    }

    @Test
    void cancel_f2Active_cancelsMatchingBooking() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant("F2")));

        BookingToken bt = new BookingToken();
        bt.setConfirmed(true);
        bt.setTenantId(TENANT_ID);
        bt.setLeadName("Anna Puig");
        bt.setMeetingAt(ZonedDateTime.of(2026, 7, 10, 14, 30, 0, 0, TZ).toInstant());

        when(bookingTokenRepository.findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any()))
                .thenReturn(List.of(bt));
        when(bookingTokenRepository.save(any())).thenReturn(bt);
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

        String result = service.handleCancel(TENANT_ID, "/cancel 2026-07-10 14:30");

        assertThat(result).contains("cancel·lada");
        verify(bookingTokenRepository).save(argThat(t -> !((BookingToken) t).isConfirmed()));
    }
}
