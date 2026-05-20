package com.amg.digitalitzacio.auth.api;

import com.amg.digitalitzacio.auth.application.SectorPromptTemplates;
import com.amg.digitalitzacio.auth.domain.BusinessSector;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
public class PromptController {

    @GetMapping("/sector/{sector}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> getSectorPrompt(@PathVariable String sector) {
        try {
            BusinessSector businessSector = BusinessSector.valueOf(sector.toUpperCase());
            String template = SectorPromptTemplates.getTemplate(businessSector);
            return ResponseEntity.ok(Map.of("sector", sector.toUpperCase(), "template", template));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
