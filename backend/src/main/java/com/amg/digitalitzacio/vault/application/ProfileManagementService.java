package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.vault.api.dto.*;
import com.amg.digitalitzacio.vault.domain.*;
import com.amg.digitalitzacio.vault.api.dto.UpdateServicePriceRequest;
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
                .cost(request.cost()).salePrice(request.salePrice())
                .monthlyPrice(request.monthlyPrice() != null ? request.monthlyPrice() : BigDecimal.TEN)
                .sortOrder(request.sortOrder())
                .build();
        catalogServiceRepository.save(svc);
        return getProfile(phase.getProfileId());
    }

    @Override
    @Transactional
    public ProfileResponse addServiceToProfile(UUID profileId, CreateServiceRequest request) {
        if (request.salePrice().compareTo(request.cost()) <= 0) {
            throw new IllegalArgumentException("salePrice must be greater than cost");
        }
        findProfile(profileId);
        var svc = CatalogService.builder()
                .profileId(profileId).name(request.name()).slug(request.slug())
                .description(request.description()).type(ServiceType.valueOf(request.type()))
                .cost(request.cost()).salePrice(request.salePrice()).sortOrder(request.sortOrder())
                .build();
        catalogServiceRepository.save(svc);
        return getProfile(profileId);
    }

    @Override
    @Transactional
    public ProfileResponse updateService(UUID serviceId, CreateServiceRequest request) {
        var svc = catalogServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
        if (request.name() != null) svc.setName(request.name());
        if (request.description() != null) svc.setDescription(request.description());
        if (request.type() != null) svc.setType(ServiceType.valueOf(request.type()));
        if (request.cost() != null) svc.setCost(request.cost());
        if (request.salePrice() != null) {
            var effectiveCost = request.cost() != null ? request.cost() : svc.getCost();
            if (request.salePrice().compareTo(effectiveCost) <= 0) {
                throw new IllegalArgumentException("salePrice must be greater than cost");
            }
            svc.setSalePrice(request.salePrice());
        }
        if (request.sortOrder() != null) svc.setSortOrder(request.sortOrder());
        catalogServiceRepository.save(svc);
        // Return the profile if it's linked, otherwise return a simple response
        if (svc.getProfileId() != null) {
            return getProfile(svc.getProfileId());
        }
        if (svc.getPhaseId() != null) {
            var phase = phaseRepository.findById(svc.getPhaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));
            return getProfile(phase.getProfileId());
        }
        // Addon — return empty response
        return new ProfileResponse(null, null, null, null, null, List.of(), List.of(), null, null);
    }

    @Override
    @Transactional
    public void deleteService(UUID serviceId) {
        var svc = catalogServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
        var fields = credentialFieldRepository.findByServiceIdOrderBySortOrder(svc.getId());
        credentialFieldRepository.deleteAll(fields);
        catalogServiceRepository.delete(svc);
    }

    @Override
    @Transactional
    public ProfileResponse createAddonService(CreateAddonServiceRequest request) {
        if (request.salePrice().compareTo(request.cost()) <= 0) {
            throw new IllegalArgumentException("salePrice must be greater than cost");
        }
        var svc = CatalogService.builder()
                .phaseId(null).profileId(null).name(request.name()).slug(request.slug())
                .description(request.description()).type(ServiceType.valueOf(request.type()))
                .cost(request.cost()).salePrice(request.salePrice()).isAddon(true)
                .monthlyPrice(request.monthlyPrice() != null ? request.monthlyPrice() : BigDecimal.TEN)
                .build();
        catalogServiceRepository.save(svc);
        return new ProfileResponse(null, null, null, null, null, List.of(), List.of(), null, null);
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

    @Override
    @Transactional
    public ProfileResponse.ServiceResponse updateServicePrice(UUID serviceId, UpdateServicePriceRequest request) {
        var svc = catalogServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
        if (request.salePrice() != null) {
            if (request.cost() != null && request.salePrice().compareTo(request.cost()) <= 0)
                throw new IllegalArgumentException("salePrice must be greater than cost");
            svc.setSalePrice(request.salePrice());
        }
        if (request.monthlyPrice() != null) svc.setMonthlyPrice(request.monthlyPrice());
        if (request.cost() != null) svc.setCost(request.cost());
        svc = catalogServiceRepository.save(svc);
        var fields = credentialFieldRepository.findByServiceIdOrderBySortOrder(svc.getId());
        return toServiceResponse(svc, fields);
    }

    private ServiceProfile findProfile(UUID id) {
        return serviceProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + id));
    }

    private ProfileResponse toProfileResponse(ServiceProfile profile, List<Phase> phases) {
        var directServices = catalogServiceRepository.findByProfileIdAndPhaseIdIsNull(profile.getId());
        return new ProfileResponse(profile.getId(), profile.getName(), profile.getSlug(),
                profile.getDescription(), profile.getIsActive(),
                directServices.stream().map(s -> toServiceResponse(s,
                        credentialFieldRepository.findByServiceIdOrderBySortOrder(s.getId()))).toList(),
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
                svc.getCost(), svc.getSalePrice(), svc.getMonthlyPrice(), svc.getSortOrder(),
                fields.stream().map(f -> new ProfileResponse.CredentialFieldResponse(
                        f.getId(), f.getKey(), f.getLabel(), f.getType(), f.getIsRequired(),
                        f.getPlaceholder(), f.getValidationRegex(), f.getSortOrder())).toList());
    }
}
