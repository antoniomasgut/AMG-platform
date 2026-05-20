package com.amg.digitalitzacio.auth.api;

import com.amg.digitalitzacio.auth.api.dto.SectorPricingResponse;
import com.amg.digitalitzacio.auth.domain.BusinessSector;
import com.amg.digitalitzacio.auth.domain.BusinessSize;
import com.amg.digitalitzacio.auth.domain.SectorPricingRepository;
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

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<SectorPricingResponse> listAll() {
        return sectorPricingRepository.findAllByOrderBySectorAscBusinessSizeAsc()
                .stream()
                .map(p -> new SectorPricingResponse(
                        p.getSector().name(),
                        p.getBusinessSize().name(),
                        p.getSetupPrice(),
                        p.getMonthlyF1(),
                        p.getMonthlyF1f2(),
                        p.getMonthlyF1f2f3(),
                        p.getMonthlyComplete()))
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
                .map(p -> ResponseEntity.ok(new SectorPricingResponse(
                        p.getSector().name(),
                        p.getBusinessSize().name(),
                        p.getSetupPrice(),
                        p.getMonthlyF1(),
                        p.getMonthlyF1f2(),
                        p.getMonthlyF1f2f3(),
                        p.getMonthlyComplete())))
                .orElse(ResponseEntity.notFound().build());
    }
}
