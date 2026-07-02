package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.FollowupLog;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantPhaseActivation;
import com.amg.digitalitzacio.auth.domain.TenantPhaseActivationRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Seqüència d'onboarding post-activació: missatge D3 i D7 al client.
 * S'activa quan un tenant rep la primera activació de fase (go-live).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OnboardingSequenceScheduler {

    private static final int WINDOW_HOURS = 12;

    private final TenantPhaseActivationRepository activationRepository;
    private final TenantRepository tenantRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final FollowupLogRepository followupLogRepository;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 10 * * *")
    public void sendOnboardingMessages() {
        sendDay(3);
        sendDay(7);
    }

    private void sendDay(int daysAfterActivation) {
        Instant now    = Instant.now();
        Instant midDay = now.minus(daysAfterActivation, ChronoUnit.DAYS);
        Instant from   = midDay.minus(WINDOW_HOURS, ChronoUnit.HOURS);
        Instant to     = midDay.plus(WINDOW_HOURS, ChronoUnit.HOURS);

        var activations = activationRepository.findByActivatedAtBetweenAndDeactivatedAtIsNull(from, to);
        if (activations.isEmpty()) return;

        log.info("[Onboarding] D{} — {} activacions a processar", daysAfterActivation, activations.size());

        // Processar un sol missatge per tenant (deduplicar independentment de quantes fases activades el mateix dia)
        var processedTenants = new java.util.HashSet<UUID>();
        for (var activation : activations) {
            UUID tenantId = activation.getTenantId();
            if (processedTenants.contains(tenantId)) continue;

            String logType = "ONBOARDING_D" + daysAfterActivation;
            if (followupLogRepository.existsByTenantIdAndTypeAndEntityId(tenantId, logType, tenantId)) {
                processedTenants.add(tenantId);
                continue;
            }

            try {
                processActivation(activation, daysAfterActivation, logType);
                processedTenants.add(tenantId);
            } catch (Exception e) {
                log.warn("[Onboarding] Error D{} tenant {}: {}", daysAfterActivation, tenantId, e.getMessage());
            }
        }
    }

    private void processActivation(TenantPhaseActivation activation, int day, String logType) {
        var tenant = tenantRepository.findById(activation.getTenantId()).orElse(null);
        if (tenant == null) return;
        // Tenant suspès (activePhases = "" explícit): no enviar consells d'onboarding
        if (tenant.getActivePhases() != null && tenant.getActivePhases().isBlank()) return;

        String clientName  = tenant.getName() != null ? tenant.getName() : "client";
        String clientEmail = tenant.getEmail();
        String clientPhone = tenant.getPhone();
        String portalUrl   = "https://amgdl.com/ca/login";

        String message = buildMessage(day, clientName, portalUrl);

        boolean sent = sendViaWhatsApp(tenant, clientPhone, message);
        if (!sent && clientEmail != null && !clientEmail.isBlank()) {
            try {
                emailService.sendEmail(clientEmail, subjectForDay(day), message);
                sent = true;
            } catch (Exception e) {
                log.warn("[Onboarding] Email D{} fallat per {}: {}", day, clientEmail, e.getMessage());
            }
        }

        if (sent) {
            // entityId = tenantId per garantir deduplicació per tenant (no per fase)
            saveLog(activation.getTenantId(), logType, activation.getTenantId());
            log.info("[Onboarding] D{} enviat a tenant {}", day, activation.getTenantId());
        }
    }

    private boolean sendViaWhatsApp(Tenant tenant, String phone, String message) {
        if (phone == null || phone.isBlank()) return false;

        var chatLink = chatLinkRepository.findByTenantId(tenant.getId()).orElse(null);
        if (chatLink == null) return false;

        if (chatLink.getWhatsappMetaPhoneNumberId() != null && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()) {
            try {
                whatsAppMetaChannel.sendMessage(chatLink.getWhatsappMetaPhoneNumberId(), phone, message);
                return true;
            } catch (Exception e) {
                log.warn("[Onboarding] WhatsApp Meta fallat: {}", e.getMessage());
            }
        }
        if (chatLink.getWhatsappPhoneNumber() != null && !chatLink.getWhatsappPhoneNumber().isBlank()) {
            try {
                whatsAppChannel.sendMessage(chatLink.getWhatsappPhoneNumber(), phone, message);
                return true;
            } catch (Exception e) {
                log.warn("[Onboarding] WhatsApp Twilio fallat: {}", e.getMessage());
            }
        }
        return false;
    }

    private String buildMessage(int day, String name, String portalUrl) {
        return switch (day) {
            case 3 -> """
                    Hola %s! 👋

                    Fa 3 dies que el teu agent IA està actiu. Com ho estàs trobant?

                    Consell D3: Revisa les converses al teu panel per veure com respon el teu agent als clients.
                    Si veus alguna resposta que milloraries, afegeix-la a la Base de Coneixement.

                    Accedeix al teu panel: %s

                    Qualsevol dubte, escriu-nos per WhatsApp!
                    L'equip AMG Digitalitzacions
                    """.formatted(name, portalUrl);
            case 7 -> """
                    Hola %s! 🚀

                    Una setmana amb el teu agent IA! 🎉

                    Consell D7: Activa les notificacions automàtiques per rebre un resum diari
                    de les converses al teu negoci.

                    Accedeix al teu panel: %s

                    Tens alguna pregunta o vols ampliar el servei? Escriu-nos per WhatsApp.
                    L'equip AMG Digitalitzacions
                    """.formatted(name, portalUrl);
            default -> "";
        };
    }

    private String subjectForDay(int day) {
        return switch (day) {
            case 3 -> "Com va el teu agent IA? Consell D3 🤖";
            case 7 -> "Una setmana amb el teu agent! Consell D7 🚀";
            default -> "Actualització del teu agent AMG";
        };
    }

    private void saveLog(UUID tenantId, String type, UUID entityId) {
        var log = new FollowupLog();
        log.setTenantId(tenantId);
        log.setType(type);
        log.setEntityId(entityId);
        followupLogRepository.save(log);
    }
}
