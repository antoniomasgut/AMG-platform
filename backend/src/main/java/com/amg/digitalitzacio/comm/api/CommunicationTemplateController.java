package com.amg.digitalitzacio.comm.api;

import com.amg.digitalitzacio.comm.api.dto.CommSendRequest;
import com.amg.digitalitzacio.comm.api.dto.CommTemplateRequest;
import com.amg.digitalitzacio.comm.application.CommunicationTemplateService;
import com.amg.digitalitzacio.comm.domain.CommunicationTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/comm")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class CommunicationTemplateController {

    private final CommunicationTemplateService service;

    @GetMapping("/templates")
    public List<CommunicationTemplate> list() {
        return service.listAll();
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunicationTemplate create(@RequestBody CommTemplateRequest req) {
        return service.create(CommunicationTemplate.builder()
                .sector(req.sector() != null ? req.sector() : "ALL")
                .channel(req.channel())
                .action(req.action())
                .language(req.language() != null ? req.language() : "ca")
                .subject(req.subject())
                .body(req.body())
                .isActive(req.isActive() != null ? req.isActive() : true)
                .sortOrder(req.sortOrder())
                .build());
    }

    @PutMapping("/templates/{id}")
    public CommunicationTemplate update(@PathVariable UUID id, @RequestBody CommTemplateRequest req) {
        return service.update(id, CommunicationTemplate.builder()
                .subject(req.subject())
                .body(req.body())
                .isActive(req.isActive())
                .sortOrder(req.sortOrder())
                .build());
    }

    @DeleteMapping("/templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/send")
    public Map<String, Object> send(@RequestBody CommSendRequest req) {
        String rendered = service.send(req.channel(), req.to(), req.action(), req.sector(), req.language(), req.variables());
        return Map.of("sent", true, "renderedBody", rendered);
    }
}
