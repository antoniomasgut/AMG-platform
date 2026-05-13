package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.vault.api.dto.*;
import com.amg.digitalitzacio.vault.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileManagementService implements ProfileService {

    private final ServiceProfileRepository serviceProfileRepository;
    private final PhaseRepository phaseRepository;
    private final CatalogServiceRepository catalogServiceRepository;
    private final CredentialFieldRepository credentialFieldRepository;

    @Override
    @Transactional
    public ProfileResponse createProfile(CreateProfileRequest request) {
        var profile = ServiceProfile.builder()
                .name(request.name()).slug(request.slug()).description(request.description())
                .build();
        profile = serviceProfileRepository.save(profile);
        return toProfileResponse(profile, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID id) {
        var profile = findProfile(id);
        var phases = phaseRepository.findByProfileIdOrderBySortOrder(id);
        return toProfileResponse(profile, phases);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponse> listProfiles() {
        var profiles = serviceProfileRepository.findAll();
        return profiles.stream().map(p -> {
            var phases = phaseRepository.findByProfileIdOrderBySortOrder(p.getId());
            return toProfileResponse(p, phases);
        }).toList();
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID id, UpdateProfileRequest request) {
        var profile = findProfile(id);
        if (request.name() != null) profile.setName(request.name());
        if (request.slug() != null) profile.setSlug(request.slug());
        if (request.description() != null) profile.setDescription(request.description());
        profile = serviceProfileRepository.save(profile);
        var phases = phaseRepository.findByProfileIdOrderBySortOrder(id);
        return toProfileResponse(profile, phases);
    }

    @Override
    @Transactional
    public void deactivateProfile(UUID id) {
        var profile = findProfile(id);
        profile.setIsActive(false);
        serviceProfileRepository.save(profile);
    }

    @Override
    @Transactional
    public ProfileResponse addPhase(UUID profileId, CreatePhaseRequest request) {
        findProfile(profileId);
        var phase = Phase.builder()
                .profileId(profileId).name(request.name())
                .description(request.description()).sortOrder(request.sortOrder())
                .build();
        phaseRepository.save(phase);
        return getProfile(profileId);
    }

    @Override
    @Transactional
    public ProfileResponse updatePhase(UUID profileId, UUID phaseId, UpdatePhaseRequest request) {
        var phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found: " + phaseId));
        if (!phase.getProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("Phase does not belong to profile");
        }
        if (request.name() != null) phase.setName(request.name());
        if (request.description() != null) phase.setDescription(request.description());
        if (request.sortOrder() != null) phase.setSortOrder(request.sortOrder());
        phaseRepository.save(phase);
        return getProfile(profileId);
    }

    @Override
    @Transactional
    public ProfileResponse deletePhase(UUID profileId, UUID phaseId) {
        var phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found: " + phaseId));
        if (!phase.getProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("Phase does not belong to profile");
        }
        var services = catalogServiceRepository.findByPhaseIdOrderBySortOrder(phaseId);
        for (var svc : services) {
            var fields = credentialFieldRepository.findByServiceIdOrderBySortOrder(svc.getId());
            credentialFieldRepository.deleteAll(fields);
        }
        catalogServiceRepository.deleteAll(services);
        phaseRepository.delete(phase);
        return getProfile(profileId);
    }

    @Override
    @Transactional
    public ProfileResponse addServiceToPhase(UUID phaseId, CreateServiceRequest request) {
        if (request.salePrice().compareTo(request.cost()) <= 0) {
            throw new IllegalArgumentException("salePrice must be greater than cost");
        }
        var phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found: " + phaseId));
        var svc = CatalogService.builder()
                .phaseId(phaseId).name(request.name()).slug(request.slug())
                .description(request.description()).type(ServiceType.valueOf(request.type()))
                .cost(request.cost()).salePrice(request.salePrice()).sortOrder(request.sortOrder())
                .build();
        catalogServiceRepository.save(svc);
        return getProfile(phase.getProfileId());
    }

    @Override
    @Transactional
    public ProfileResponse createAddonService(CreateAddonServiceRequest request) {
        if (request.salePrice().compareTo(request.cost()) <= 0) {
            throw new IllegalArgumentException("salePrice must be greater than cost");
        }
        var svc = CatalogService.builder()
                .phaseId(null).name(request.name()).slug(request.slug())
                .description(request.description()).type(ServiceType.valueOf(request.type()))
                .cost(request.cost()).salePrice(request.salePrice()).isAddon(true)
                .build();
        svc = catalogServiceRepository.save(svc);
        var fields = credentialFieldRepository.findByServiceIdOrderBySortOrder(svc.getId());
        var svcResp = toServiceResponse(svc, fields);
        var phaseResp = new ProfileResponse.PhaseResponse(null, "Add-on", null, 0, List.of(svcResp));
        return new ProfileResponse(null, null, null, null, null, List.of(phaseResp), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponse.ServiceResponse> listServices() {
        return catalogServiceRepository.findAll().stream()
                .map(s -> {
                    var fields = credentialFieldRepository.findByServiceIdOrderBySortOrder(s.getId());
                    return toServiceResponse(s, fields);
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse calculateBudget(UUID profileId, List<UUID> addonIds, boolean includeCost) {
        var profile = findProfile(profileId);
        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profileId);
        var addons = addonIds != null ? catalogServiceRepository.findAllById(addonIds) : List.<CatalogService>of();

        var budgetPhases = new ArrayList<BudgetResponse.BudgetPhase>();
        var total = BigDecimal.ZERO;
        var totalCost = BigDecimal.ZERO;

        for (var phase : phases) {
            var services = catalogServiceRepository.findByPhaseIdOrderBySortOrder(phase.getId());
            var budgetServices = new ArrayList<BudgetResponse.BudgetPhase.BudgetService>();
            var phaseTotal = BigDecimal.ZERO;
            var phaseCost = BigDecimal.ZERO;

            for (var svc : services) {
                phaseTotal = phaseTotal.add(svc.getSalePrice());
                phaseCost = phaseCost.add(svc.getCost());
                budgetServices.add(new BudgetResponse.BudgetPhase.BudgetService(
                        svc.getId(), svc.getName(), svc.getSalePrice(),
                        includeCost ? svc.getCost() : null));
            }

            budgetPhases.add(new BudgetResponse.BudgetPhase(
                    new BudgetResponse.BudgetPhase.PhaseRef(phase.getId(), phase.getName(), phase.getSortOrder()),
                    budgetServices, phaseTotal,
                    includeCost ? phaseCost : null,
                    includeCost ? phaseTotal.subtract(phaseCost) : null));
            total = total.add(phaseTotal);
            totalCost = totalCost.add(phaseCost);
        }

        var budgetAddons = new ArrayList<BudgetResponse.BudgetAddon>();
        for (var addon : addons) {
            budgetAddons.add(new BudgetResponse.BudgetAddon(addon.getId(), addon.getName(), addon.getSalePrice(),
                    includeCost ? addon.getCost() : null));
            total = total.add(addon.getSalePrice());
            totalCost = totalCost.add(addon.getCost());
        }

        return new BudgetResponse(
                new BudgetResponse.ProfileRef(profile.getId(), profile.getName()),
                budgetPhases, budgetAddons, total,
                includeCost ? totalCost : null,
                includeCost ? total.subtract(totalCost) : null);
    }

    private ServiceProfile findProfile(UUID id) {
        return serviceProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + id));
    }

    private ProfileResponse toProfileResponse(ServiceProfile profile, List<Phase> phases) {
        return new ProfileResponse(profile.getId(), profile.getName(), profile.getSlug(),
                profile.getDescription(), profile.getIsActive(),
                phases.stream().map(this::toPhaseResponse).toList(),
                profile.getCreatedAt(), profile.getUpdatedAt());
    }

    private ProfileResponse.PhaseResponse toPhaseResponse(Phase phase) {
        var services = catalogServiceRepository.findByPhaseIdOrderBySortOrder(phase.getId());
        return new ProfileResponse.PhaseResponse(phase.getId(), phase.getName(), phase.getDescription(),
                phase.getSortOrder(),
                services.stream().map(s -> {
                    var fields = credentialFieldRepository.findByServiceIdOrderBySortOrder(s.getId());
                    return toServiceResponse(s, fields);
                }).toList());
    }

    private ProfileResponse.ServiceResponse toServiceResponse(CatalogService svc, List<CredentialField> fields) {
        return new ProfileResponse.ServiceResponse(svc.getId(), svc.getName(), svc.getSlug(),
                svc.getDescription(), svc.getType(), svc.getIsAddon(),
                svc.getCost(), svc.getSalePrice(), svc.getSortOrder(),
                fields.stream().map(f -> new ProfileResponse.CredentialFieldResponse(
                        f.getId(), f.getKey(), f.getLabel(), f.getType(), f.getIsRequired(),
                        f.getPlaceholder(), f.getValidationRegex(), f.getSortOrder())).toList());
    }
}
