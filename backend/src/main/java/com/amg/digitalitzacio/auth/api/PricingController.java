package com.amg.digitalitzacio.auth.api;

import com.amg.digitalitzacio.auth.api.dto.SectorPhaseResponse;
import com.amg.digitalitzacio.auth.api.dto.SectorPricingResponse;
import com.amg.digitalitzacio.auth.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final SectorPricingRepository sectorPricingRepository;
    private final SectorPhaseRepository sectorPhaseRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<SectorPricingResponse> listAll() {
        return sectorPricingRepository.findAllByOrderBySectorAscBusinessSizeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SectorPricingResponse> lookup(
            @RequestParam String sector,
            @RequestParam String size) {
        return sectorPricingRepository
                .findBySectorAndBusinessSize(
                        BusinessSector.valueOf(sector.toUpperCase()),
                        BusinessSize.valueOf(size.toUpperCase()))
                .map(p -> ResponseEntity.ok(toResponse(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/phases")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SectorPhaseResponse>> phases(@RequestParam String sector) {
        try {
            var s = BusinessSector.valueOf(sector.toUpperCase());
            var phases = sectorPhaseRepository.findBySectorOrderByPhaseNumber(s)
                    .stream().map(this::toPhaseResponse).toList();
            return phases.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(phases);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/worker-tiers")
    @PreAuthorize("isAuthenticated()")
    public List<WorkerTierDto> workerTiers() {
        return List.of(
            new WorkerTierDto("AUTONOMO", "Autònom (1 persona)", 0, 0),
            new WorkerTierDto("PETIT",    "Equip petit (2-3 persones)", 99, 20),
            new WorkerTierDto("MITJA",    "Equip mitjà (4-6 persones)", 199, 35),
            new WorkerTierDto("EMPRESA",  "Empresa (7+ persones)", 299, 50)
        );
    }

    public record WorkerTierDto(String size, String label, int setupAddon, int monthlyAddon) {}

    private SectorPhaseResponse toPhaseResponse(SectorPhase p) {
        return new SectorPhaseResponse(
                p.getSector().name(),
                p.getPhaseNumber(),
                p.getName(),
                p.getDescription(),
                p.getDependencyType().name(),
                SectorPhaseResponse.parseRequired(p.getRequiredPhases()),
                p.getSetupPrice(),
                p.getMonthlyPrice());
    }

    private SectorPricingResponse toResponse(com.amg.digitalitzacio.auth.domain.SectorPricing p) {
        return new SectorPricingResponse(
                p.getSector().name(),
                p.getBusinessSize().name(),
                p.getSetupPrice(),
                p.getPriceF1(),
                p.getPriceF2(),
                p.getPriceF3(),
                p.getPriceF4(),
                p.getPriceF5(),
                p.getSetupF2(),
                p.getSetupF3(),
                p.getSetupF4(),
                p.getSetupF5());
    }
}
