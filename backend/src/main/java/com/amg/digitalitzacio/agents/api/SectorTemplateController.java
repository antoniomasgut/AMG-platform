package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.api.dto.SectorTemplateRequest;
import com.amg.digitalitzacio.agents.api.dto.SectorTemplateResponse;
import com.amg.digitalitzacio.agents.domain.SectorTemplate;
import com.amg.digitalitzacio.agents.domain.SectorTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sector-templates")
@RequiredArgsConstructor
public class SectorTemplateController {

    private final SectorTemplateRepository repository;

    @GetMapping
    public List<SectorTemplateResponse> list(
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String type) {
        List<SectorTemplate> results;
        if (sector != null && type != null) {
            results = repository.findBySectorAndTypeOrderBySortOrder(sector.toUpperCase(), type);
        } else if (sector != null) {
            results = repository.findBySectorOrderBySortOrder(sector.toUpperCase());
        } else if (type != null) {
            results = repository.findByTypeOrderBySortOrder(type);
        } else {
            results = repository.findAll();
        }
        return results.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SectorTemplateResponse get(@PathVariable UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template no trobat"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SectorTemplateResponse update(@PathVariable UUID id, @RequestBody SectorTemplateRequest req) {
        var tpl = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template no trobat"));
        if (req.title() != null) tpl.setTitle(req.title());
        if (req.body() != null) tpl.setBody(req.body());
        return toResponse(repository.save(tpl));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SectorTemplateResponse> create(@RequestBody SectorTemplateRequest req) {
        // Not a real endpoint for now — creation done via seeder
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private SectorTemplateResponse toResponse(SectorTemplate t) {
        return new SectorTemplateResponse(t.getId(), t.getSector(), t.getType(), t.getTitle(), t.getBody(), t.getSortOrder());
    }
}
