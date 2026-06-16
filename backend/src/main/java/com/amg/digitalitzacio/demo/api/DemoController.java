package com.amg.digitalitzacio.demo.api;

import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.demo.api.dto.DemoFlowResponse;
import com.amg.digitalitzacio.demo.api.dto.DemoListResponse;
import com.amg.digitalitzacio.demo.application.DemoService;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoService demoService;
    private final LeadRepository leadRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final WhatsAppChannel whatsAppChannel;

    @GetMapping
    public DemoListResponse listDemos() {
        return demoService.listDemos();
    }

    @GetMapping("/{id}")
    public DemoFlowResponse getDemo(@PathVariable String id) {
        return demoService.getDemo(id);
    }

    /**
     * Envia la URL de demo per WhatsApp al lead indicat.
     */
    @PostMapping("/{id}/send-whatsapp")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, Object>> sendDemoWhatsApp(
            @PathVariable String id,
            @RequestParam UUID leadId,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Obté la demo (llança 404 si no existeix)
        demoService.getDemo(id);

        // Obté el lead
        var lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead no trobat: " + leadId));

        // Valida telèfon
        if (lead.getPhone() == null || lead.getPhone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El lead no té número de telèfon");
        }

        String demoUrl = "https://amgdl.com/demo/inbox/" + id;
        String firstName = lead.getName() != null ? lead.getName().split(" ")[0] : "client";
        String message = "Hola " + firstName + "! T'enviem una demostració del nostre sistema d'agent IA. "
                + "Pots parlar-hi directament aquí:\n" + demoUrl
                + "\n\nEt mostrarà com funciona el nostre agent per a negocis com el teu.";

        // Obté la configuració de canal del tenant
        var chatLink = chatLinkRepository.findByTenantId(principal.tenantId()).orElse(null);

        boolean sent = false;

        if (chatLink != null
                && chatLink.getWhatsappMetaPhoneNumberId() != null
                && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()) {
            whatsAppMetaChannel.sendMessage(chatLink.getWhatsappMetaPhoneNumberId(), lead.getPhone(), message);
            sent = true;
        } else if (chatLink != null
                && chatLink.getWhatsappPhoneNumber() != null
                && !chatLink.getWhatsappPhoneNumber().isBlank()) {
            whatsAppChannel.sendMessage(chatLink.getWhatsappPhoneNumber(), lead.getPhone(), message);
            sent = true;
        }

        if (!sent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No hi ha cap canal de WhatsApp configurat per a aquest tenant");
        }

        return ResponseEntity.ok(Map.of("sent", true, "phone", lead.getPhone()));
    }
}
