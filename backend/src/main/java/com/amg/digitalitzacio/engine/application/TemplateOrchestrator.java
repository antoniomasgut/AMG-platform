package com.amg.digitalitzacio.engine.application;

import com.amg.digitalitzacio.engine.api.dto.*;
import com.amg.digitalitzacio.engine.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemplateOrchestrator implements TemplateService {

    private final LandingTemplateRepository templateRepository;
    private final TemplateSectionRepository sectionRepository;

    @Override
    @Transactional
    public LandingTemplateResponse createTemplate(CreateTemplateRequest request) {
        var template = LandingTemplate.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .build();
        template = templateRepository.save(template);
        return toFullResponse(template, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandingTemplateSummary> listTemplates() {
        return templateRepository.findAll().stream()
                .map(t -> new LandingTemplateSummary(
                        t.getId(), t.getName(), t.getSlug(), t.getDescription(),
                        sectionRepository.findByTemplateIdOrderBySortOrder(t.getId()).size(),
                        t.getIsActive(), t.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LandingTemplateResponse getTemplate(UUID id) {
        var template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        var sections = sectionRepository.findByTemplateIdOrderBySortOrder(template.getId());
        return toFullResponse(template, sections);
    }

    @Override
    @Transactional
    public LandingTemplateResponse updateTemplate(UUID id, CreateTemplateRequest request) {
        var template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        if (request.name() != null) template.setName(request.name());
        if (request.slug() != null) template.setSlug(request.slug());
        if (request.description() != null) template.setDescription(request.description());
        template = templateRepository.save(template);
        var sections = sectionRepository.findByTemplateIdOrderBySortOrder(template.getId());
        return toFullResponse(template, sections);
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID id) {
        var template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        // Soft delete: marcar com a inactiu
        template.setIsActive(false);
        templateRepository.save(template);
    }

    @Override
    @Transactional
    public LandingTemplateResponse addSection(UUID templateId, TemplateSectionRequest request) {
        templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        var section = TemplateSection.builder()
                .templateId(templateId)
                .blockType(BlockType.valueOf(request.blockType()))
                .sortOrder(request.sortOrder())
                .propsSchema(request.propsSchema())
                .defaultProps(request.defaultProps())
                .build();
        section = sectionRepository.save(section);
        var sections = sectionRepository.findByTemplateIdOrderBySortOrder(templateId);
        var template = templateRepository.findById(templateId).orElseThrow();
        return toFullResponse(template, sections);
    }

    @Override
    @Transactional
    public LandingTemplateResponse updateSection(UUID templateId, UUID sectionId, TemplateSectionRequest request) {
        templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        var section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
        if (request.blockType() != null) section.setBlockType(BlockType.valueOf(request.blockType()));
        if (request.sortOrder() != null) section.setSortOrder(request.sortOrder());
        if (request.propsSchema() != null) section.setPropsSchema(request.propsSchema());
        if (request.defaultProps() != null) section.setDefaultProps(request.defaultProps());
        sectionRepository.save(section);
        var sections = sectionRepository.findByTemplateIdOrderBySortOrder(templateId);
        var template = templateRepository.findById(templateId).orElseThrow();
        return toFullResponse(template, sections);
    }

    @Override
    @Transactional
    public void removeSection(UUID templateId, UUID sectionId) {
        templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        sectionRepository.deleteById(sectionId);
    }

    private LandingTemplateResponse toFullResponse(LandingTemplate template, List<TemplateSection> sections) {
        var sectionViews = sections.stream()
                .map(s -> new LandingTemplateSectionView(
                        s.getId(), s.getBlockType().name(), s.getSortOrder(),
                        s.getPropsSchema(), s.getDefaultProps()))
                .toList();
        return new LandingTemplateResponse(
                template.getId(), template.getName(), template.getSlug(),
                template.getDescription(), template.getIsActive(),
                sectionViews, template.getCreatedAt(), template.getUpdatedAt());
    }
}
