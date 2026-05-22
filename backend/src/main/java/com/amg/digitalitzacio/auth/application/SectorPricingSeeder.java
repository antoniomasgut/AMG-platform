package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.auth.domain.BusinessSector;
import com.amg.digitalitzacio.auth.domain.BusinessSize;
import com.amg.digitalitzacio.auth.domain.SectorPricing;
import com.amg.digitalitzacio.auth.domain.SectorPricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(2)
@Profile("!test")
@RequiredArgsConstructor
public class SectorPricingSeeder implements CommandLineRunner {

    private final SectorPricingRepository sectorPricingRepository;

    @Override
    public void run(String... args) {
        // Migra si existeixen files antigues sense priceF1 (model bundle → model per fase)
        boolean needsMigration = sectorPricingRepository.count() > 0
                && sectorPricingRepository.findAll().stream()
                        .anyMatch(p -> p.getPriceF1() == null);

        if (sectorPricingRepository.count() > 0 && !needsMigration) return;

        if (needsMigration) sectorPricingRepository.deleteAll();

        // Paràmetres: sector, mida, setup, f1, f2, f3, f4, f5
        // f1 = preu base · f2,f3,f4,f5 = increment per cada fase addicional
        sectorPricingRepository.saveAll(List.of(
            entry(BusinessSector.PINTOR,             BusinessSize.AUTONOMO, 150, 59, 20, 20, 30, 20),
            entry(BusinessSector.PINTOR,             BusinessSize.PETIT,    250, 79, 20, 30, 30, 20),
            entry(BusinessSector.ELECTRICISTA,       BusinessSize.AUTONOMO, 150, 59, 20, 20, 30, 20),
            entry(BusinessSector.ELECTRICISTA,       BusinessSize.PETIT,    250, 79, 20, 30, 30, 20),
            entry(BusinessSector.FONTANER,           BusinessSize.AUTONOMO, 150, 59, 20, 20, 30, 20),
            entry(BusinessSector.JARDINER,           BusinessSize.AUTONOMO, 150, 59, 20, 20, 30, 20),
            entry(BusinessSector.NETEJA,             BusinessSize.AUTONOMO, 150, 59, 20, 20, 30, 20),
            entry(BusinessSector.NETEJA,             BusinessSize.MITJA,    300, 89, 30, 30, 30, 20),
            entry(BusinessSector.FISIOTERAPEUTA,     BusinessSize.AUTONOMO, 175, 69, 20, 20, 30, 20),
            entry(BusinessSector.FISIOTERAPEUTA,     BusinessSize.PETIT,    275, 99, 30, 40, 30, 20),
            entry(BusinessSector.FISIOTERAPEUTA,     BusinessSize.MITJA,    375, 129,40, 60, 50, 20),
            entry(BusinessSector.PSICOLEG,           BusinessSize.AUTONOMO, 175, 69, 20, 20, 30, 20),
            entry(BusinessSector.PSICOLEG,           BusinessSize.PETIT,    300, 109,40, 40, 40, 20),
            entry(BusinessSector.NUTRICIONISTA,      BusinessSize.AUTONOMO, 175, 59, 20, 20, 30, 20),
            entry(BusinessSector.PERRUQUERIA,        BusinessSize.AUTONOMO, 150, 59, 20, 20, 30, 20),
            entry(BusinessSector.PERRUQUERIA,        BusinessSize.PETIT,    300, 99, 40, 40, 40, 20),
            entry(BusinessSector.ESTETICA,           BusinessSize.AUTONOMO, 150, 59, 20, 20, 30, 20),
            entry(BusinessSector.ESTETICA,           BusinessSize.MITJA,    350, 109,40, 40, 40, 20),
            entry(BusinessSector.GESTORIA,           BusinessSize.AUTONOMO, 200, 69, 20, 20, 30, 20),
            entry(BusinessSector.GESTORIA,           BusinessSize.MITJA,    400, 109,40, 50, 50, 20),
            entry(BusinessSector.ACADEMIA,           BusinessSize.AUTONOMO, 175, 59, 20, 20, 30, 20),
            entry(BusinessSector.ACADEMIA,           BusinessSize.MITJA,    300, 99, 40, 30, 40, 20),
            entry(BusinessSector.TALLER_MECANIC,     BusinessSize.PETIT,    150, 59, 20, 20, 30, 20),
            entry(BusinessSector.TALLER_MECANIC,     BusinessSize.MITJA,    275, 89, 30, 30, 30, 20),
            entry(BusinessSector.VETERINARI,         BusinessSize.AUTONOMO, 175, 69, 20, 20, 30, 20),
            entry(BusinessSector.VETERINARI,         BusinessSize.PETIT,    325, 109,40, 40, 40, 20),
            entry(BusinessSector.PERRUQUERIA_CANINA, BusinessSize.AUTONOMO, 150, 49, 20, 20, 30, 20)
        ));
    }

    private SectorPricing entry(BusinessSector sector, BusinessSize size,
                                int setup, int f1, int f2, int f3, int f4, int f5) {
        return SectorPricing.builder()
                .sector(sector)
                .businessSize(size)
                .setupPrice(BigDecimal.valueOf(setup))
                .priceF1(BigDecimal.valueOf(f1))
                .priceF2(BigDecimal.valueOf(f2))
                .priceF3(BigDecimal.valueOf(f3))
                .priceF4(BigDecimal.valueOf(f4))
                .priceF5(BigDecimal.valueOf(f5))
                .build();
    }
}
