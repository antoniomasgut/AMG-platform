package com.amg.digitalitzacio.comm.application;

import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.comm.domain.CommunicationTemplate;
import com.amg.digitalitzacio.comm.domain.CommunicationTemplateRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunicationTemplateServiceTest {

    @Mock private CommunicationTemplateRepository repo;
    @Mock private EmailService emailService;
    @Mock private WhatsAppChannel whatsAppChannel;
    @Mock private SystemConfigService systemConfigService;

    @InjectMocks private CommunicationTemplateService service;

    private CommunicationTemplate allCaTpl;
    private CommunicationTemplate sectorEsTpl;

    @BeforeEach
    void setUp() {
        allCaTpl = CommunicationTemplate.builder()
                .sector("ALL").channel("EMAIL").action("DEMO_SEND").language("ca")
                .subject("Demo").body("Hola {NAME}, la teva demo és {URL}")
                .isActive(true).sortOrder(0).build();

        sectorEsTpl = CommunicationTemplate.builder()
                .sector("PERRUQUERIA").channel("EMAIL").action("DEMO_SEND").language("es")
                .subject("Demo").body("Hola {NAME}, tu demo está en {URL}")
                .isActive(true).sortOrder(0).build();
    }

    // ── resolve() ─────────────────────────────────────────────────────────────

    @Test
    void resolve_returnsSectorSpecificTemplate_whenExists() {
        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
                "PERRUQUERIA", "EMAIL", "DEMO_SEND", "es")).thenReturn(Optional.of(sectorEsTpl));

        var result = service.resolve("PERRUQUERIA", "EMAIL", "DEMO_SEND", "es");

        assertThat(result).isEqualTo(sectorEsTpl);
    }

    @Test
    void resolve_fallsBackToAllSector_whenSectorSpecificAbsent() {
        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
                "PERRUQUERIA", "EMAIL", "DEMO_SEND", "es")).thenReturn(Optional.empty());
        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
                "ALL", "EMAIL", "DEMO_SEND", "es")).thenReturn(Optional.of(allCaTpl));

        var result = service.resolve("PERRUQUERIA", "EMAIL", "DEMO_SEND", "es");

        assertThat(result).isEqualTo(allCaTpl);
    }

    @Test
    void resolve_fallsBackToCaLocale_whenLanguageAbsent() {
        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
                "PERRUQUERIA", "EMAIL", "DEMO_SEND", "de")).thenReturn(Optional.empty());
        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
                "ALL", "EMAIL", "DEMO_SEND", "de")).thenReturn(Optional.empty());
        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
                "ALL", "EMAIL", "DEMO_SEND", "ca")).thenReturn(Optional.of(allCaTpl));

        var result = service.resolve("PERRUQUERIA", "EMAIL", "DEMO_SEND", "de");

        assertThat(result).isEqualTo(allCaTpl);
    }

    @Test
    void resolve_throws404_whenNoFallbackExists() {
        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("INEXISTENT", "EMAIL", "DEMO_SEND", "ca"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void resolve_usesNullSector_asAll() {
        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
                "ALL", "EMAIL", "DEMO_SEND", "ca")).thenReturn(Optional.of(allCaTpl));

        var result = service.resolve(null, "EMAIL", "DEMO_SEND", "ca");

        assertThat(result).isEqualTo(allCaTpl);
    }

    @Test
    void resolve_usesNullLanguage_asCa() {
        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
                "ALL", "EMAIL", "DEMO_SEND", "ca")).thenReturn(Optional.of(allCaTpl));

        var result = service.resolve("ALL", "EMAIL", "DEMO_SEND", null);

        assertThat(result).isEqualTo(allCaTpl);
    }

    // ── render() ──────────────────────────────────────────────────────────────

    @Test
    void render_substitutesAllVariables() {
        String template = "Hola {NAME}, la teva cita és el {DATE} a les {TIME}";
        String result = service.render(template, Map.of("NAME", "Maria", "DATE", "15/06", "TIME", "10:00"));

        assertThat(result).isEqualTo("Hola Maria, la teva cita és el 15/06 a les 10:00");
    }

    @Test
    void render_keepsUnknownVariables_unchanged() {
        String template = "Hola {NAME}, empresa: {COMPANY_UNKNOWN}";
        String result = service.render(template, Map.of("NAME", "Joan"));

        // Variables no presents al map queden sense substituir
        assertThat(result).isEqualTo("Hola Joan, empresa: {COMPANY_UNKNOWN}");
    }

    @Test
    void render_handlesEmptyVariables() {
        String template = "Text sense variables";
        String result = service.render(template, Map.of());

        assertThat(result).isEqualTo("Text sense variables");
    }

    // ── send() ────────────────────────────────────────────────────────────────

    @Test
    void send_email_callsEmailService() {
        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
                "ALL", "EMAIL", "DEMO_SEND", "ca")).thenReturn(Optional.of(allCaTpl));

        service.send("EMAIL", "client@test.com", "DEMO_SEND", "ALL", "ca",
                Map.of("NAME", "Joan", "URL", "https://demo.amgdl.com/demo-abc"));

        verify(emailService).sendEmail(
                eq("client@test.com"),
                eq("Demo"),
                contains("Joan"));
    }

    @Test
    void send_whatsapp_callsWhatsAppChannel() {
        var waTpl = CommunicationTemplate.builder()
                .sector("ALL").channel("WHATSAPP").action("DEMO_SEND").language("ca")
                .body("Hola {NAME}: {URL}").isActive(true).sortOrder(0).build();

        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
                "ALL", "WHATSAPP", "DEMO_SEND", "ca")).thenReturn(Optional.of(waTpl));
        when(systemConfigService.get("TWILIO_WHATSAPP_FROM")).thenReturn("+34600000000");

        service.send("WHATSAPP", "+34611111111", "DEMO_SEND", "ALL", "ca",
                Map.of("NAME", "Maria", "URL", "https://demo.amgdl.com"));

        verify(whatsAppChannel).sendMessage(eq("+34600000000"), eq("+34611111111"), contains("Maria"));
    }

    @Test
    void send_unknownChannel_throws400() {
        when(repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(any(), any(), any(), any()))
                .thenReturn(Optional.of(allCaTpl));

        assertThatThrownBy(() -> service.send("SMS", "test@test.com", "DEMO_SEND", "ALL", "ca", Map.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }
}
