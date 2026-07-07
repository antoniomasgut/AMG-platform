package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.booking.domain.BookingToken;
import com.amg.digitalitzacio.booking.domain.BookingTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantAgendaQueryServiceTest {

    @Mock BookingTokenRepository bookingTokenRepository;
    @Mock TenantRepository tenantRepository;

    @InjectMocks TenantAgendaQueryService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final ZoneId TZ = ZoneId.of("Europe/Madrid");

    private Tenant tenantWithF2() {
        return Tenant.builder().id(TENANT_ID).name("Test").slug("test")
                .contractedPhases("F2").build();
    }

    @BeforeEach
    void setUp() {
        when(bookingTokenRepository.findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void returnsGateMessage_whenTenantNotFound() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        String result = service.handleCommand(TENANT_ID, "/agenda");

        assertThat(result).contains("no està activada");
        verifyNoInteractions(bookingTokenRepository);
    }

    @Test
    void returnsGateMessage_whenF2NotActive() {
        Tenant t = Tenant.builder().id(TENANT_ID).name("Test").slug("test")
                .contractedPhases("F1").build();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(t));

        String result = service.handleCommand(TENANT_ID, "/agenda");

        assertThat(result).contains("no està activada");
        verifyNoInteractions(bookingTokenRepository);
    }

    @Test
    void handleCommand_noArg_queriesForToday() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));

        String result = service.handleCommand(TENANT_ID, "/agenda");

        assertThat(result).contains("Avui");
        verify(bookingTokenRepository).findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any());
    }

    @Test
    void handleCommand_avui_queriesForToday() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));

        String result = service.handleCommand(TENANT_ID, "/agenda avui");

        assertThat(result).contains("Avui");
        verify(bookingTokenRepository).findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any());
    }

    @Test
    void handleCommand_dema_queriesForTomorrow() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));

        String result = service.handleCommand(TENANT_ID, "/agenda demà");

        assertThat(result).contains("Demà");
        verify(bookingTokenRepository).findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any());
    }

    @Test
    void handleCommand_setmana_queriesRange() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));

        String result = service.handleCommand(TENANT_ID, "/agenda setmana");

        assertThat(result).contains("7 dies");
        verify(bookingTokenRepository).findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any());
    }

    @Test
    void handleCommand_specificDate_queriesForThatDay() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));

        String result = service.handleCommand(TENANT_ID, "/agenda 2026-07-10");

        assertThat(result).contains("10/07/2026");
        verify(bookingTokenRepository).findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any());
    }

    @Test
    void handleCommand_withBookings_returnsFormattedList() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));

        BookingToken bt = new BookingToken();
        bt.setConfirmed(true);
        bt.setLeadName("Maria García");
        bt.setMeetingAt(ZonedDateTime.of(2026, 7, 10, 10, 30, 0, 0, TZ).toInstant());
        bt.setTenantId(TENANT_ID);

        when(bookingTokenRepository.findByTenantIdAndMeetingAtBetween(eq(TENANT_ID), any(), any()))
                .thenReturn(List.of(bt));

        String result = service.handleCommand(TENANT_ID, "/agenda 2026-07-10");

        assertThat(result).contains("Maria García");
        assertThat(result).contains("10:30");
    }

    @Test
    void handleCommand_invalidArg_returnsHelp() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithF2()));

        String result = service.handleCommand(TENANT_ID, "/agenda blabla");

        assertThat(result).contains("No entenc");
        assertThat(result).contains("/agenda demà");
    }
}
