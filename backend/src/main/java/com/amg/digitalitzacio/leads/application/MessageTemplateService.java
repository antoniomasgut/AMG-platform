package com.amg.digitalitzacio.leads.application;

import com.amg.digitalitzacio.leads.domain.MessageTemplate;
import com.amg.digitalitzacio.leads.domain.MessageTemplateRepository;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageTemplateService {

    // DTO d'entrada per a crear i actualitzar plantilles
    public record TemplateRequest(String name, String type, String sector, String subject, String body) {}

    private final MessageTemplateRepository templateRepository;

    /** Retorna totes les plantilles del tenant autenticat, ordenades per tipus i nom. */
    public List<MessageTemplate> list(UserPrincipal principal) {
        return templateRepository.findByTenantIdOrderByTypeAscNameAsc(principal.tenantId());
    }

    /** Crea una nova plantilla associada al tenant del principal. */
    public MessageTemplate create(TemplateRequest request, UserPrincipal principal) {
        MessageTemplate template = MessageTemplate.builder()
                .tenantId(principal.tenantId())
                .name(request.name())
                .type(request.type())
                .sector(request.sector())
                .subject(request.subject())
                .body(request.body())
                .build();
        return templateRepository.save(template);
    }

    /**
     * Actualitza una plantilla existent. Només modifica els camps no-null del request
     * per permetre actualitzacions parcials.
     */
    public MessageTemplate update(UUID id, TemplateRequest request, UserPrincipal principal) {
        MessageTemplate template = templateRepository.findByIdAndTenantId(id, principal.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        if (request.name() != null)    template.setName(request.name());
        if (request.type() != null)    template.setType(request.type());
        if (request.sector() != null)  template.setSector(request.sector());
        if (request.subject() != null) template.setSubject(request.subject());
        if (request.body() != null)    template.setBody(request.body());

        return templateRepository.save(template);
    }

    /** Elimina una plantilla del tenant autenticat. */
    public void delete(UUID id, UserPrincipal principal) {
        MessageTemplate template = templateRepository.findByIdAndTenantId(id, principal.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        templateRepository.delete(template);
    }
}
