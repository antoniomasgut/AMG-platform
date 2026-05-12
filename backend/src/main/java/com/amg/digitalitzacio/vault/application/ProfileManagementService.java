package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.api.dto.*;
import com.amg.digitalitzacio.vault.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileManagementService implements ProfileService {

    private final ServiceProfileRepository profileRepository;
    private final PhaseRepository phaseRepository;
    private final ServiceRepository serviceRepository;
    private final CredentialFieldRepository fieldRepository;

    // ── Perfils ──

    @Override
    @Transactional
    public ProfileResponse createProfile(CreateProfileRequest request) {
        if (profileRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new IllegalArgumentException("Ja existeix un perfil amb aquest slug");
        }
        var profile = ServiceProfile.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .isActive(true)
                .build();
        profile = profileRepository.save(profile);
        return toProfileResponse(profile, List.of());
    }

    @Override
    public ProfileResponse getProfile(UUID id) {
        var profile = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no trobat"));
        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profile.getId());
        return toProfileResponse(profile, phases);
    }

    @Override
    public List<ProfileResponse> listProfiles() {
        return profileRepository.findByIsActiveTrue().stream()
                .map(profile -> {
                    var phases = phaseRepository.findByProfileIdOrderBySortOrder(profile.getId());
                    return toProfileResponse(profile, phases);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID id, UpdateProfileRequest request) {
        var profile = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no trobat"));
        if (request.getName() != null) profile.setName(request.getName());
        if (request.getSlug() != null) {
            if (profileRepository.findBySlug(request.getSlug()).isPresent()) {
                throw new IllegalArgumentException("Ja existeix un perfil amb aquest slug");
            }
            profile.setSlug(request.getSlug());
        }
        if (request.getDescription() != null) profile.setDescription(request.getDescription());
        profile = profileRepository.save(profile);
        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profile.getId());
        return toProfileResponse(profile, phases);
    }

    @Override
    @Transactional
    public void deactivateProfile(UUID id) {
        var profile = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no trobat"));
        profile.setIsActive(false);
        profileRepository.save(profile);
    }

    // ── Fases ──

    @Override
    @Transactional
    public ProfileResponse addPhase(UUID profileId, CreatePhaseRequest request) {
        var profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no trobat"));
        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profileId);
        var maxOrder = phases.stream().mapToInt(Phase::getSortOrder).max().orElse(0);
        var phase = Phase.builder()
                .profileId(profileId)
                .name(request.getName())
                .description(request.getDescription())
                .sortOrder(maxOrder + 1)
                .build();
        phaseRepository.save(phase);
        phases = phaseRepository.findByProfileIdOrderBySortOrder(profileId);
        return toProfileResponse(profile, phases);
    }

    @Override
    @Transactional
    public ProfileResponse updatePhase(UUID profileId, UUID phaseId, UpdatePhaseRequest request) {
        var profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no trobat"));
        var phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase no trobada"));
        if (!phase.getProfileId().equals(profileId)) {
            throw new IllegalArgumentException("La fase no pertany a aquest perfil");
        }
        if (request.getName() != null) phase.setName(request.getName());
        if (request.getDescription() != null) phase.setDescription(request.getDescription());
        phaseRepository.save(phase);
        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profileId);
        return toProfileResponse(profile, phases);
    }

    @Override
    @Transactional
    public void deletePhase(UUID profileId, UUID phaseId) {
        var phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase no trobada"));
        if (!phase.getProfileId().equals(profileId)) {
            throw new IllegalArgumentException("La fase no pertany a aquest perfil");
        }
        phaseRepository.delete(phase);
    }

    // ── Serveis ──

    @Override
    @Transactional
    public ProfileResponse addServiceToPhase(UUID phaseId, CreateServiceRequest request) {
        var phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase no trobada"));
        validatePricing(request.getCost(), request.getSalePrice());
        var service = CatalogService.builder()
                .phaseId(phaseId)
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .type(request.getType())
                .isAddon(request.getIsAddon() != null && request.getIsAddon())
                .cost(request.getCost())
                .salePrice(request.getSalePrice())
                .sortOrder(request.getSortOrder())
                .build();
        serviceRepository.save(service);
        return buildFullProfileResponse(phase.getProfileId());
    }

    @Override
    @Transactional
    public ServiceResponse createAddonService(CreateAddonServiceRequest request) {
        validatePricing(request.getCost(), request.getSalePrice());
        var service = CatalogService.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .type(request.getType())
                .isAddon(true)
                .cost(request.getCost())
                .salePrice(request.getSalePrice())
                .build();
        service = serviceRepository.save(service);
        return toServiceResponse(service);
    }

    @Override
    public List<ServiceResponse> listServices() {
        return serviceRepository.findAll().stream()
                .map(this::toServiceResponse)
                .collect(Collectors.toList());
    }

    // ── Pressupost ──

    @Override
    public BudgetResponse calculateBudget(UUID profileId, List<UUID> addonIds, boolean includeCost) {
        var profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no trobat"));
        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profileId);

        var budgetPhases = phases.stream().map(phase -> {
            var services = serviceRepository.findByPhaseIdOrderBySortOrder(phase.getId());
            var phaseTotal = services.stream().map(CatalogService::getSalePrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            var phaseCost = services.stream().map(CatalogService::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            return BudgetResponse.PhaseBudget.builder()
                    .phase(BudgetResponse.PhaseBudget.PhaseRef.builder()
                            .id(phase.getId()).name(phase.getName()).sortOrder(phase.getSortOrder()).build())
                    .services(services.stream().map(s -> buildServiceResponse(s, includeCost)).toList())
                    .phaseTotal(phaseTotal)
                    .phaseCost(includeCost ? phaseCost : null)
                    .phaseMargin(includeCost ? phaseTotal.subtract(phaseCost) : null)
                    .build();
        }).toList();

        List<CatalogService> addons = addonIds != null ? addonIds.stream()
                .map(id -> serviceRepository.findById(id).orElse(null))
                .filter(s -> s != null && s.getIsAddon())
                .toList() : Collections.emptyList();

        var total = budgetPhases.stream().map(BudgetResponse.PhaseBudget::getPhaseTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(addons.stream().map(CatalogService::getSalePrice).reduce(BigDecimal.ZERO, BigDecimal::add));

        var totalCost = budgetPhases.stream()
                .map(p -> includeCost ? p.getPhaseCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(addons.stream().map(CatalogService::getCost).reduce(BigDecimal.ZERO, BigDecimal::add));

        return BudgetResponse.builder()
                .profile(BudgetResponse.ProfileRef.builder().id(profile.getId()).name(profile.getName()).build())
                .phases(budgetPhases)
                .addons(addons.stream().map(a -> buildServiceResponse(a, includeCost)).toList())
                .total(total)
                .totalCost(includeCost ? totalCost : null)
                .totalMargin(includeCost ? total.subtract(totalCost) : null)
                .build();
    }

    // ── Privats ──

    private void validatePricing(BigDecimal cost, BigDecimal salePrice) {
        if (salePrice.compareTo(cost) <= 0) {
            throw new IllegalArgumentException("El preu de venda ha de ser major que el cost");
        }
    }

    private ProfileResponse buildFullProfileResponse(UUID profileId) {
        var profile = profileRepository.findById(profileId).orElseThrow();
        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profileId);
        return toProfileResponse(profile, phases);
    }

    private ProfileResponse toProfileResponse(ServiceProfile profile, List<Phase> phases) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .name(profile.getName())
                .slug(profile.getSlug())
                .description(profile.getDescription())
                .isActive(profile.getIsActive())
                .phases(phases.stream().map(this::toPhaseResponse).toList())
                .build();
    }

    private PhaseResponse toPhaseResponse(Phase phase) {
        var services = serviceRepository.findByPhaseIdOrderBySortOrder(phase.getId());
        return PhaseResponse.builder()
                .id(phase.getId())
                .name(phase.getName())
                .description(phase.getDescription())
                .sortOrder(phase.getSortOrder())
                .services(services.stream().map(this::toServiceResponse).toList())
                .build();
    }

    private ServiceResponse toServiceResponse(CatalogService service) {
        return buildServiceResponse(service, true);
    }

    private ServiceResponse buildServiceResponse(CatalogService service, boolean includeCost) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .slug(service.getSlug())
                .description(service.getDescription())
                .type(service.getType())
                .isAddon(service.getIsAddon())
                .cost(includeCost ? service.getCost() : null)
                .salePrice(service.getSalePrice())
                .sortOrder(service.getSortOrder())
                .build();
    }
}
