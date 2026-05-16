package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.api.dto.*;

import java.util.List;
import java.util.UUID;

public interface ProfileService {
    ProfileResponse createProfile(CreateProfileRequest request);
    ProfileResponse getProfile(UUID id);
    ProfileResponse updateProfile(UUID id, UpdateProfileRequest request);
    void deactivateProfile(UUID id);
    ProfileResponse addPhase(UUID profileId, CreatePhaseRequest request);
    ProfileResponse updatePhase(UUID profileId, UUID phaseId, UpdatePhaseRequest request);
    ProfileResponse deletePhase(UUID profileId, UUID phaseId);
    ProfileResponse addServiceToPhase(UUID phaseId, CreateServiceRequest request);
    ProfileResponse addServiceToProfile(UUID profileId, CreateServiceRequest request);
    ProfileResponse updateService(UUID serviceId, CreateServiceRequest request);
    void deleteService(UUID serviceId);
    ProfileResponse createAddonService(CreateAddonServiceRequest request);
    List<ProfileResponse> listProfiles();
    List<ProfileResponse.ServiceResponse> listServices();
    BudgetResponse calculateBudget(UUID profileId, List<UUID> addonIds, boolean includeCost);
    ProfileResponse.ServiceResponse updateServicePrice(UUID serviceId, UpdateServicePriceRequest request);
}
