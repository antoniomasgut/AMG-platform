package com.amg.digitalitzacio.leads.application;

import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.leads.api.dto.*;
import com.amg.digitalitzacio.leads.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.notification.NotificationEvent;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.amg.digitalitzacio.shared.notification.TenantNotificationService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LeadService {

    private final LeadRepository leadRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final MessageTemplateRepository messageTemplateRepository;
    private final WhatsAppChannel whatsAppChannel;
    private final SystemConfigService sysConfig;
    private final TenantNotificationService notificationService;
    private final AIProviderRouter aiProviderRouter;
    private final ObjectMapper objectMapper;

    public LeadResponse createLead(LeadRequest request, UserPrincipal principal) {
        UUID tenantId = principal.tenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required to create a lead");
        }

        Lead lead = new Lead();
        lead.setTenantId(tenantId);
        lead.setName(request.name());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setSource(request.source() != null ? request.source() : LeadSource.OTHER);
        lead.setStage(PipelineStage.NEW);
        lead.setAssignedTo(request.assignedTo());
        lead.setEstimatedValue(request.estimatedValue());
        lead.setNotes(request.notes());
        lead.setTags(request.tags());
        lead.setUtmSource(request.utmSource());
        lead.setUtmMedium(request.utmMedium());
        lead.setUtmCampaign(request.utmCampaign());

        lead = leadRepository.save(lead);

        String contact = request.email() != null ? request.email()
                : request.phone() != null ? request.phone() : "—";
        notificationService.notify(tenantId, NotificationEvent.LEAD_CREATED, Map.of(
                "nom",     request.name() != null ? request.name() : "—",
                "contact", contact,
                "stage",   "Nou"));

        return toLeadResponse(lead);
    }

    public Page<LeadResponse> listLeads(Pageable pageable, String stage, String source,
                                        UUID assignedTo, String search, UUID tenantIdFilter,
                                        UserPrincipal principal) {
        String role = principal.role();
        UUID tenantId = "SUPER_ADMIN".equals(role) && tenantIdFilter != null ? tenantIdFilter : principal.tenantId();

        Page<Lead> page;
        if (search != null && !search.isBlank()) {
            if (tenantId != null) {
                page = leadRepository.findByTenantIdAndNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
                        tenantId, search, search, search, pageable);
            } else {
                // SUPER_ADMIN searching all tenants
                page = leadRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
                        search, search, search, pageable);
            }
        } else if (stage != null && !stage.isBlank()) {
            PipelineStage ps = PipelineStage.valueOf(stage);
            page = tenantId != null
                    ? leadRepository.findByTenantIdAndStage(tenantId, ps, pageable)
                    : leadRepository.findByStage(ps, pageable);
        } else if (source != null && !source.isBlank()) {
            LeadSource ls = LeadSource.valueOf(source);
            page = tenantId != null
                    ? leadRepository.findByTenantIdAndSource(tenantId, ls, pageable)
                    : leadRepository.findBySource(ls, pageable);
        } else if (assignedTo != null) {
            page = tenantId != null
                    ? leadRepository.findByTenantIdAndAssignedTo(tenantId, assignedTo, pageable)
                    : leadRepository.findByAssignedTo(assignedTo, pageable);
        } else {
            page = tenantId != null
                    ? leadRepository.findByTenantId(tenantId, pageable)
                    : leadRepository.findAll(pageable);
        }
        return page.map(this::toLeadResponse);
    }

    public LeadResponse getLead(UUID id, UserPrincipal principal) {
        Lead lead = findLead(id);
        verifyAccess(lead, principal);
        return toLeadResponse(lead);
    }

    public LeadResponse updateLead(UUID id, LeadRequest request, UserPrincipal principal) {
        Lead lead = findLead(id);
        verifyAccess(lead, principal);

        if (request.name() != null) lead.setName(request.name());
        if (request.email() != null) lead.setEmail(request.email());
        if (request.phone() != null) lead.setPhone(request.phone());
        if (request.source() != null) lead.setSource(request.source());
        if (request.assignedTo() != null) lead.setAssignedTo(request.assignedTo());
        if (request.estimatedValue() != null) lead.setEstimatedValue(request.estimatedValue());
        if (request.notes() != null) lead.setNotes(request.notes());
        if (request.tags() != null) lead.setTags(request.tags());
        if (request.hasWhatsapp() != null) lead.setHasWhatsapp(request.hasWhatsapp());

        lead = leadRepository.save(lead);
        return toLeadResponse(lead);
    }

    public void deleteLead(UUID id, UserPrincipal principal) {
        String role = principal.role();
        if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("Only SUPER_ADMIN or ADMIN can delete leads");
        }
        Lead lead = findLead(id);
        verifyAccess(lead, principal);
        lead.setIsActive(false);
        leadRepository.save(lead);
    }

    public LeadResponse changeStage(UUID id, StageChangeRequest request, UserPrincipal principal) {
        Lead lead = findLead(id);
        verifyAccess(lead, principal);

        PipelineStage newStage = request.stage();
        if (newStage == PipelineStage.LOST && (request.lostReason() == null || request.lostReason().isBlank())) {
            throw new IllegalArgumentException("lostReason is required when stage is LOST");
        }

        // Reopen from WON/LOST
        if (lead.getStage() == PipelineStage.WON || lead.getStage() == PipelineStage.LOST) {
            lead.setConvertedAt(null);
        }

        if (newStage == PipelineStage.WON) {
            lead.setConvertedAt(Instant.now());
        } else {
            lead.setConvertedAt(null);
        }

        if (newStage == PipelineStage.LOST) {
            lead.setLostReason(request.lostReason());
        }

        lead.setStage(newStage);
        lead = leadRepository.save(lead);

        // Auto-create activity
        String desc = switch (newStage) {
            case WON -> "Lead guanyat";
            case LOST -> "Lead perdut: " + request.lostReason();
            default -> "Lead canviat a " + newStage.name();
        };
        Activity activity = new Activity();
        activity.setLeadId(lead.getId());
        activity.setUserId(principal.id());
        activity.setType(ActivityType.NOTE);
        activity.setDescription(desc);
        activityRepository.save(activity);

        return toLeadResponse(lead);
    }

    public LeadStatsResponse getLeadStats(UserPrincipal principal) {
        UUID tenantId = principal.tenantId();
        if ("SUPER_ADMIN".equals(principal.role())) {
            tenantId = null;
        }

        long total = tenantId != null
                ? leadRepository.countByTenantId(tenantId)
                : leadRepository.count();

        Map<PipelineStage, Long> byStage = new HashMap<>();
        for (PipelineStage s : PipelineStage.values()) {
            long count = tenantId != null
                    ? leadRepository.countByTenantIdAndStage(tenantId, s)
                    : leadRepository.countByStage(s);
            byStage.put(s, count);
        }

        Map<LeadSource, Long> bySource = new HashMap<>();
        for (LeadSource s : LeadSource.values()) {
            long count = tenantId != null
                    ? leadRepository.countByTenantIdAndSource(tenantId, s)
                    : leadRepository.countBySource(s);
            bySource.put(s, count);
        }

        long won = byStage.getOrDefault(PipelineStage.WON, 0L);
        double conversionRate = total > 0 ? (double) won / total : 0.0;

        return new LeadStatsResponse(total, byStage, bySource, conversionRate);
    }

    // --- RGPD: dret d'esborrat (Art. 17) ---

    public void purgeLeadPersonalData(UUID id, UserPrincipal principal) {
        String role = principal.role();
        if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("Only SUPER_ADMIN or ADMIN can purge leads");
        }
        Lead lead = findLead(id);
        verifyAccess(lead, principal);

        // Pseudoanonimitza les dades personals; conserva el registre per traçabilitat
        lead.setName("[Dades esborrades - RGPD]");
        lead.setEmail(null);
        lead.setPhone(null);
        lead.setNotes(null);
        lead.setTags(null);
        lead.setIsActive(false);
        leadRepository.save(lead);

        // Elimina activitats (poden contenir dades personals en la descripció)
        activityRepository.deleteByLeadId(id);
    }

    public void purgeAllTenantLeadPersonalData(UserPrincipal principal) {
        if (!"SUPER_ADMIN".equals(principal.role()) && !"ADMIN".equals(principal.role())) {
            throw new IllegalArgumentException("Only SUPER_ADMIN or ADMIN can bulk purge leads");
        }
        UUID tenantId = principal.tenantId();
        List<Lead> leads = leadRepository.findByTenantId(tenantId);
        for (Lead lead : leads) {
            lead.setName("[Dades esborrades - RGPD]");
            lead.setEmail(null);
            lead.setPhone(null);
            lead.setNotes(null);
            lead.setTags(null);
            lead.setIsActive(false);
            activityRepository.deleteByLeadId(lead.getId());
        }
        leadRepository.saveAll(leads);
    }

    // --- RGPD: portabilitat de dades (Art. 20) ---

    public List<LeadResponse> exportLeadsForPortability(UserPrincipal principal) {
        UUID tenantId = principal.tenantId();
        return leadRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toLeadResponse)
                .toList();
    }

    public LeadResponse setWhatsapp(UUID id, boolean value, UserPrincipal principal) {
        Lead lead = findLead(id);
        verifyAccess(lead, principal);
        lead.setHasWhatsapp(value);
        return toLeadResponse(leadRepository.save(lead));
    }

    public Map<String, Integer> sendOutreach(OutreachRequest request, UserPrincipal principal) {
        int sent = 0;
        String url = request.demoUrl() != null ? request.demoUrl() : "";
        for (UUID leadId : request.leadIds()) {
            Lead lead = leadRepository.findById(leadId).orElse(null);
            if (lead == null || lead.getEmail() == null || lead.getEmail().isBlank()) continue;
            verifyAccess(lead, principal);

            String subject = request.subject().replace("{{nom}}", lead.getName());
            String body = request.body()
                    .replace("{{nom}}", lead.getName())
                    .replace("{{demoUrl}}", url);
            try {
                emailService.sendEmail(lead.getEmail(), subject, body);
                sent++;

                if (lead.getStage() == PipelineStage.NEW) {
                    lead.setStage(PipelineStage.CONTACTED);
                    leadRepository.save(lead);
                }

                Activity activity = new Activity();
                activity.setLeadId(leadId);
                activity.setUserId(principal.id());
                activity.setType(ActivityType.EMAIL);
                activity.setDescription("Correu de demo enviat: " + subject);
                activityRepository.save(activity);
            } catch (Exception e) {
                // Continua amb el següent lead si un falla
            }
        }
        return Map.of("sent", sent);
    }

    public Map<String, Integer> sendTemplate(SendTemplateRequest request, UserPrincipal principal) {
        var template = messageTemplateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + request.templateId()));

        boolean isWhatsapp = "WHATSAPP".equalsIgnoreCase(request.channel())
                || "WHATSAPP".equalsIgnoreCase(template.getType());
        String fromWa = sysConfig.get("TWILIO_WHATSAPP_FROM");

        int sent = 0, failed = 0;
        for (UUID leadId : request.leadIds()) {
            Lead lead = leadRepository.findById(leadId).orElse(null);
            if (lead == null) { failed++; continue; }
            verifyAccess(lead, principal);

            String body    = substitute(template.getBody(), lead);
            String subject = template.getSubject() != null ? substitute(template.getSubject(), lead) : template.getName();

            try {
                if (isWhatsapp) {
                    if (lead.getPhone() == null || lead.getPhone().isBlank()) { failed++; continue; }
                    whatsAppChannel.sendMessage(
                        fromWa != null ? fromWa : "",
                        normalizePhone(lead.getPhone()),
                        body
                    );
                    logActivity(leadId, principal.id(), ActivityType.WHATSAPP,
                            "WhatsApp enviat [" + template.getName() + "]: " + body.substring(0, Math.min(80, body.length())));
                } else {
                    if (lead.getEmail() == null || lead.getEmail().isBlank()) { failed++; continue; }
                    emailService.sendEmail(lead.getEmail(), subject, body);
                    logActivity(leadId, principal.id(), ActivityType.EMAIL,
                            "Email enviat [" + template.getName() + "]: " + subject);
                }
                if (lead.getStage() == PipelineStage.NEW) {
                    lead.setStage(PipelineStage.CONTACTED);
                    leadRepository.save(lead);
                }
                sent++;
            } catch (Exception e) {
                log.warn("sendTemplate failed for lead {}: {}", leadId, e.getMessage());
                failed++;
            }
        }
        return Map.of("sent", sent, "failed", failed);
    }

    private String substitute(String text, Lead lead) {
        return text
                .replace("{{nom}}", lead.getName() != null ? lead.getName() : "")
                .replace("{{email}}", lead.getEmail() != null ? lead.getEmail() : "")
                .replace("{{telefon}}", lead.getPhone() != null ? lead.getPhone() : "");
    }

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 9 ? "+34" + digits : "+" + digits;
    }

    private void logActivity(UUID leadId, UUID userId, ActivityType type, String description) {
        Activity activity = new Activity();
        activity.setLeadId(leadId);
        activity.setUserId(userId);
        activity.setType(type);
        activity.setDescription(description);
        activityRepository.save(activity);
    }

    private Lead findLead(UUID id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
    }

    private void verifyAccess(Lead lead, UserPrincipal principal) {
        if ("SUPER_ADMIN".equals(principal.role())) return;
        if (lead.getTenantId() == null || !lead.getTenantId().equals(principal.tenantId())) {
            throw new ResourceNotFoundException("Lead not found: " + lead.getId());
        }
    }

    private LeadResponse toLeadResponse(Lead lead) {
        return new LeadResponse(
                lead.getId(), lead.getName(), lead.getEmail(), lead.getPhone(),
                lead.getSource(), lead.getStage(),
                getUserRef(lead.getAssignedTo()),
                lead.getEstimatedValue(), lead.getNotes(), lead.getTags(),
                lead.getLostReason(), lead.getConvertedAt(),
                lead.getLastContactAt(), lead.getLastServiceAt(), lead.getHasWhatsapp(),
                lead.getUtmSource(), lead.getUtmMedium(), lead.getUtmCampaign(), lead.getMetaLeadId(),
                lead.getIsActive(), lead.getCreatedAt(), lead.getUpdatedAt()
        );
    }

    private LeadResponse.UserRef getUserRef(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(u -> new LeadResponse.UserRef(u.getId(), u.getName()))
                .orElse(null);
    }

    public AnalyzeNotesResponse analyzeNotes(UUID id, AnalyzeNotesRequest request, UserPrincipal principal) {
        Lead lead = findLead(id);
        verifyAccess(lead, principal);

        // Guardem les notes si n'hi ha de noves
        if (request.notes() != null && !request.notes().isBlank()) {
            lead.setNotes(request.notes());
            leadRepository.save(lead);
        }

        String systemPrompt = """
            Ets un expert en vendes i digitalització de negocis locals per a l'agència AMG Digitalització.
            La teva tasca és analitzar les notes preses durant una entrevista amb un client potencial (lead) i identificar les seves necessitats.
            
            Retorna ÚNICAMENT un objecte JSON amb la següent estructura i cap altre text:
            {
              "painPoints": ["Punt 1", "Punt 2"], // Llista dels principals problemes o dolors detectats
              "recommendedSector": "Un dels valors: RESTAURANTE, PINTOR, PELUQUERIA, ABOGADO, CLINICA, TALLER, OTRO",
              "recommendedSize": "Un dels valors: AUTONOMO, PETIT, MITJA, EMPRESA",
              "setupAmount": 500, // Import de setup recomanat en euros (ex: 200, 500, 1000) depenent de la complexitat
              "monthlyAmount": 50, // Import de quota mensual recomanada en euros (ex: 30, 50, 150)
              "recommendationPitch": "Breu text de 2-3 frases per vendre-li la proposta"
            }
            """;

        String userMessage = "Aquestes són les notes de l'entrevista amb el client:\n" + request.notes();

        var aiProvider = aiProviderRouter.forModel(aiProviderRouter.defaultModel());
        String responseText = aiProvider.chat(systemPrompt, List.of(), userMessage);

        try {
            // Eliminar possibles blocs de markdown (```json ... ```) de la resposta
            String cleanJson = responseText.replaceAll("(?s)^```json\\s*", "").replaceAll("(?s)\\s*```$", "");
            
            Map<String, Object> parsed = objectMapper.readValue(cleanJson, new TypeReference<>() {});
            
            List<String> painPoints = parsed.get("painPoints") instanceof List<?> list ? (List<String>) list : List.of();
            String sector = parsed.get("recommendedSector") instanceof String s ? s : "OTRO";
            String size = parsed.get("recommendedSize") instanceof String s ? s : "AUTONOMO";
            BigDecimal setup = parsed.get("setupAmount") instanceof Number n ? new BigDecimal(n.toString()) : BigDecimal.ZERO;
            BigDecimal monthly = parsed.get("monthlyAmount") instanceof Number n ? new BigDecimal(n.toString()) : BigDecimal.ZERO;
            String pitch = parsed.get("recommendationPitch") instanceof String s ? s : "";
            
            // També registrem que s'ha analitzat per IA
            Activity activity = new Activity();
            activity.setLeadId(lead.getId());
            activity.setUserId(principal.id());
            activity.setType(ActivityType.NOTE);
            activity.setDescription("Notes analitzades per IA. Dolors: " + String.join(", ", painPoints));
            activityRepository.save(activity);

            return new AnalyzeNotesResponse(painPoints, sector, size, setup, monthly, pitch);
        } catch (Exception e) {
            log.error("Error parsejant la resposta de la IA: {}", responseText, e);
            throw new RuntimeException("No s'ha pogut analitzar les notes amb la IA. Torna a intentar-ho.");
        }
    }
}
