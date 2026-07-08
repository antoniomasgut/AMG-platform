package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.booking.application.AvailabilityService;
import com.amg.digitalitzacio.booking.application.BookingService;
import com.amg.digitalitzacio.booking.domain.BookingToken;
import com.amg.digitalitzacio.booking.domain.BookingTokenRepository;
import com.amg.digitalitzacio.google.application.GoogleBusinessHoursService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbsenceRescheduleServiceTest {

    @Mock ScheduledAgentTaskRepository taskRepository;
    @Mock AbsenceRecordRepository absenceRepository;
    @Mock TenantChatLinkRepository chatLinkRepository;
    @Mock TenantRepository tenantRepository;
    @Mock NexeServiceConfigService nexeConfigService;
    @Mock BookingTokenRepository bookingTokenRepository;
    @Mock BookingService bookingService;
    @Mock AvailabilityService availabilityService;
    @Mock WhatsAppChannel whatsAppChannel;
    @Mock WhatsAppMetaChannel whatsAppMetaChannel;
    @Mock TelegramBotClient telegramBotClient;
    @Mock ObjectMapper objectMapper;
    @Mock GoogleBusinessHoursService googleBusinessHoursService;

    @InjectMocks AbsenceRescheduleService service;

    private static final UUID TENANT_ID = UUID.randomUUID();

    private Tenant tenantWithF2() {
        return Tenant.builder().id(TENANT_ID).name("Test").slug("test")
                .contractedPhases("F2").build();
    }

    private void stubCascadeForNoBookings() {
        when(taskRepository.findByTenantIdAndAgentSlugAndStatusAndScheduledAtBetween(
                eq(TENANT_ID), anyString(), any(), any(), any()))
                .thenReturn(List.of());
        when(bookingTokenRepository.findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any()))
                .thenReturn(List.of());
        when(absenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ── F2 gate ───────────────────────────────────────────────────────────────

    @Test
    void singleDay_f2NotActive_returnsGatedMessage() {
        Tenant t = Tenant.builder().id(TENANT_ID).name("Test").slug("test")
                .contractedPhases("F1").build();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(t));

        String result = service.handleAbsenceCommand(TENANT_ID, "/absencia 2026-07-10", null, null);

        assertThat(result).contains("Agenda (F2) activada");
        verifyNoInteractions(bookingTokenRepository);
    }

    // ── Single day ─────────────────────────────────────────────────────────────

    @Test
    void singleDay_queuesOneCascade() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        stubCascadeForNoBookings();

        String result = service.handleAbsenceCommand(TENANT_ID, "/absencia 2026-07-10", null, null);

        assertThat(result).contains("Absència registrada");
        assertThat(result).contains("2026-07-10");
        verify(bookingTokenRepository, times(1))
                .findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any());
        verify(absenceRepository, times(1)).save(any());
    }

    @Test
    void singleDay_avui_works() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        stubCascadeForNoBookings();

        String result = service.handleAbsenceCommand(TENANT_ID, "/absencia avui", null, null);

        assertThat(result).contains("Absència registrada");
        verify(bookingTokenRepository, times(1))
                .findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any());
    }

    // ── Multi-day range ────────────────────────────────────────────────────────

    @Test
    void range_queriesCascadeForEachDay() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        stubCascadeForNoBookings();

        // 2026-07-14 to 2026-07-20 = 7 days
        String result = service.handleAbsenceCommand(
                TENANT_ID, "/absencia 2026-07-14 al 2026-07-20", null, null);

        assertThat(result).contains("2026-07-14");
        assertThat(result).contains("2026-07-20");
        assertThat(result).contains("Dies processats: 7");
        verify(bookingTokenRepository, times(7))
                .findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any());
        verify(absenceRepository, times(7)).save(any());
    }

    @Test
    void range_singleDayRange_works() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        stubCascadeForNoBookings();

        String result = service.handleAbsenceCommand(
                TENANT_ID, "/absencia 2026-07-10 al 2026-07-10", null, null);

        assertThat(result).contains("Dies processats: 1");
        verify(bookingTokenRepository, times(1))
                .findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any());
    }

    @Test
    void range_endBeforeStart_returnsError() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));

        String result = service.handleAbsenceCommand(
                TENANT_ID, "/absencia 2026-07-20 al 2026-07-14", null, null);

        assertThat(result).contains("data final ha de ser posterior");
        verifyNoInteractions(bookingTokenRepository);
    }

    @Test
    void range_moreThan30Days_returnsError() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));

        String result = service.handleAbsenceCommand(
                TENANT_ID, "/absencia 2026-07-01 al 2026-08-31", null, null);

        assertThat(result).contains("rang màxim");
        verifyNoInteractions(bookingTokenRepository);
    }

    @Test
    void range_f2NotActive_returnsGatedMessage() {
        Tenant t = Tenant.builder().id(TENANT_ID).name("Test").slug("test")
                .contractedPhases("F1").build();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(t));

        String result = service.handleAbsenceCommand(
                TENANT_ID, "/absencia 2026-07-14 al 2026-07-20", null, null);

        assertThat(result).contains("Agenda (F2) activada");
        verifyNoInteractions(bookingTokenRepository);
    }

    // ── Invalid format ─────────────────────────────────────────────────────────

    @Test
    void invalidDate_returnsFormatHelp() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));

        String result = service.handleAbsenceCommand(TENANT_ID, "/absencia bla", null, null);

        assertThat(result).contains("Format de data no reconegut");
        assertThat(result).contains("2026-07-14 al 2026-07-20");
    }

    // ── Google Business special hours (Mòdul 57 F1) ───────────────────────────

    @Test
    void singleDay_gbpConfigured_appendsGoogleLine() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        stubCascadeForNoBookings();
        when(googleBusinessHoursService.markClosed(eq(TENANT_ID), any(), any())).thenReturn(true);

        String result = service.handleAbsenceCommand(TENANT_ID, "/absencia 2026-07-10", null, null);

        assertThat(result).contains("tancat a Google");
        verify(googleBusinessHoursService).markClosed(TENANT_ID,
                java.time.LocalDate.parse("2026-07-10"), java.time.LocalDate.parse("2026-07-10"));
    }

    @Test
    void singleDay_gbpNotConfigured_noGoogleLine() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        stubCascadeForNoBookings();
        when(googleBusinessHoursService.markClosed(eq(TENANT_ID), any(), any())).thenReturn(false);

        String result = service.handleAbsenceCommand(TENANT_ID, "/absencia 2026-07-10", null, null);

        assertThat(result).contains("Absència registrada");
        assertThat(result).doesNotContain("tancat a Google");
    }

    @Test
    void range_gbpConfigured_marksWholeRangeOnce() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        stubCascadeForNoBookings();
        when(googleBusinessHoursService.markClosed(eq(TENANT_ID), any(), any())).thenReturn(true);

        String result = service.handleAbsenceCommand(TENANT_ID, "/absencia 2026-07-14 al 2026-07-16", null, null);

        assertThat(result).contains("tancat a Google");
        verify(googleBusinessHoursService, times(1)).markClosed(TENANT_ID,
                java.time.LocalDate.parse("2026-07-14"), java.time.LocalDate.parse("2026-07-16"));
    }

    @Test
    void singleDay_gbpThrows_summaryStillReturned() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        stubCascadeForNoBookings();
        when(googleBusinessHoursService.markClosed(eq(TENANT_ID), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        String result = service.handleAbsenceCommand(TENANT_ID, "/absencia 2026-07-10", null, null);

        assertThat(result).contains("Absència registrada");
        assertThat(result).doesNotContain("tancat a Google");
    }
}
