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
        if (sectorPricingRepository.count() > 0) {
            return;
        }

        sectorPricingRepository.saveAll(List.of(
                entry(BusinessSector.PINTOR,             BusinessSize.AUTONOMO, 150, 59,  79,  99,  129),
                entry(BusinessSector.PINTOR,             BusinessSize.PETIT,    250, 79,  99,  129, 159),
                entry(BusinessSector.ELECTRICISTA,       BusinessSize.AUTONOMO, 150, 59,  79,  99,  129),
                entry(BusinessSector.ELECTRICISTA,       BusinessSize.PETIT,    250, 79,  99,  129, 159),
                entry(BusinessSector.FONTANER,           BusinessSize.AUTONOMO, 150, 59,  79,  99,  129),
                entry(BusinessSector.JARDINER,           BusinessSize.AUTONOMO, 150, 59,  79,  99,  129),
                entry(BusinessSector.NETEJA,             BusinessSize.AUTONOMO, 150, 59,  79,  99,  129),
                entry(BusinessSector.NETEJA,             BusinessSize.MITJA,    300, 89,  119, 149, 179),
                entry(BusinessSector.FISIOTERAPEUTA,     BusinessSize.AUTONOMO, 175, 69,  89,  109, 139),
                entry(BusinessSector.FISIOTERAPEUTA,     BusinessSize.PETIT,    275, 99,  129, 169, 199),
                entry(BusinessSector.FISIOTERAPEUTA,     BusinessSize.MITJA,    375, 129, 169, 229, 279),
                entry(BusinessSector.PSICOLEG,           BusinessSize.AUTONOMO, 175, 69,  89,  109, 139),
                entry(BusinessSector.PSICOLEG,           BusinessSize.PETIT,    300, 109, 149, 189, 229),
                entry(BusinessSector.NUTRICIONISTA,      BusinessSize.AUTONOMO, 175, 59,  79,  99,  129),
                entry(BusinessSector.PERRUQUERIA,        BusinessSize.AUTONOMO, 150, 59,  79,  99,  129),
                entry(BusinessSector.PERRUQUERIA,        BusinessSize.PETIT,    300, 99,  139, 179, 219),
                entry(BusinessSector.ESTETICA,           BusinessSize.AUTONOMO, 150, 59,  79,  99,  129),
                entry(BusinessSector.ESTETICA,           BusinessSize.MITJA,    350, 109, 149, 189, 229),
                entry(BusinessSector.GESTORIA,           BusinessSize.AUTONOMO, 200, 69,  89,  109, 139),
                entry(BusinessSector.GESTORIA,           BusinessSize.MITJA,    400, 109, 149, 199, 249),
                entry(BusinessSector.ACADEMIA,           BusinessSize.AUTONOMO, 175, 59,  79,  99,  129),
                entry(BusinessSector.ACADEMIA,           BusinessSize.MITJA,    300, 99,  139, 169, 209),
                entry(BusinessSector.TALLER_MECANIC,     BusinessSize.PETIT,    150, 59,  79,  99,  129),
                entry(BusinessSector.TALLER_MECANIC,     BusinessSize.MITJA,    275, 89,  119, 149, 179),
                entry(BusinessSector.VETERINARI,         BusinessSize.AUTONOMO, 175, 69,  89,  109, 139),
                entry(BusinessSector.VETERINARI,         BusinessSize.PETIT,    325, 109, 149, 189, 229),
                entry(BusinessSector.PERRUQUERIA_CANINA, BusinessSize.AUTONOMO, 150, 49,  69,  89,  119)
        ));
    }

    private SectorPricing entry(BusinessSector sector, BusinessSize size,
                                int setup, int f1, int f1f2, int f1f2f3, int complete) {
        return SectorPricing.builder()
                .sector(sector)
                .businessSize(size)
                .setupPrice(BigDecimal.valueOf(setup))
                .monthlyF1(BigDecimal.valueOf(f1))
                .monthlyF1f2(BigDecimal.valueOf(f1f2))
                .monthlyF1f2f3(BigDecimal.valueOf(f1f2f3))
                .monthlyComplete(BigDecimal.valueOf(complete))
                .build();
    }
}
