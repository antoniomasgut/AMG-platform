package com.amg.digitalitzacio.engine.application;

import com.amg.digitalitzacio.engine.api.dto.*;

import java.util.List;
import java.util.UUID;

public interface TemplateService {
    LandingTemplateResponse createTemplate(CreateTemplateRequest request);
    List<LandingTemplateSummary> listTemplates();
    LandingTemplateResponse getTemplate(UUID id);
    LandingTemplateResponse updateTemplate(UUID id, CreateTemplateRequest request);
    void deleteTemplate(UUID id);
    LandingTemplateResponse addSection(UUID templateId, TemplateSectionRequest request);
    LandingTemplateResponse updateSection(UUID templateId, UUID sectionId, TemplateSectionRequest request);
    void removeSection(UUID templateId, UUID sectionId);
}
