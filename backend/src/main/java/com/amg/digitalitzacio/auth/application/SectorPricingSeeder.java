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
        // Re-seed si la taula és buida o si priceF1 és 0 (primera versió no tenia preus per fase)
        boolean needsReseed = sectorPricingRepository.count() == 0
                || sectorPricingRepository.findAll().stream()
                        .anyMatch(p -> p.getPriceF1() == null
                                || BigDecimal.ZERO.compareTo(p.getPriceF1()) == 0);

        if (!needsReseed) return;

        sectorPricingRepository.deleteAll();

        // Paràmetres: sector, mida, setup, f1, f2, f3, f4, f5
        // f1 = preu base mensual (1 fase)
        // f2..f5 = increment mensual per cada fase addicional (independent de quina fase sigui)
        // Verificat contra preus-nexelocal.pdf v1.0 (Maig 2026)
        sectorPricingRepository.saveAll(List.of(
            // Serveis a la llar
            entry(BusinessSector.PINTOR,             BusinessSize.AUTONOMO, 150,  59, 20, 20, 15, 15),
            entry(BusinessSector.PINTOR,             BusinessSize.PETIT,    250,  79, 20, 30, 15, 15),
            entry(BusinessSector.ELECTRICISTA,       BusinessSize.AUTONOMO, 150,  59, 20, 20, 15, 15),
            entry(BusinessSector.ELECTRICISTA,       BusinessSize.PETIT,    250,  79, 20, 30, 15, 15),
            entry(BusinessSector.FONTANER,           BusinessSize.AUTONOMO, 150,  59, 20, 20, 15, 15),
            entry(BusinessSector.JARDINER,           BusinessSize.AUTONOMO, 150,  59, 20, 20, 15, 15),
            entry(BusinessSector.NETEJA,             BusinessSize.AUTONOMO, 150,  59, 20, 20, 15, 15),
            entry(BusinessSector.NETEJA,             BusinessSize.MITJA,    350, 119, 30, 30, 15, 15),
            // Salut i benestar
            entry(BusinessSector.FISIOTERAPEUTA,     BusinessSize.AUTONOMO, 175,  69, 20, 20, 15, 15),
            entry(BusinessSector.FISIOTERAPEUTA,     BusinessSize.PETIT,    275,  99, 30, 40, 15, 15),
            entry(BusinessSector.FISIOTERAPEUTA,     BusinessSize.MITJA,    375, 129, 40, 60, 25, 25),
            entry(BusinessSector.PSICOLEG,           BusinessSize.AUTONOMO, 175,  69, 20, 20, 15, 15),
            entry(BusinessSector.PSICOLEG,           BusinessSize.PETIT,    300, 109, 40, 40, 20, 20),
            entry(BusinessSector.NUTRICIONISTA,      BusinessSize.AUTONOMO, 175,  59, 20, 20, 15, 15),
            // Estètica i bellesa
            entry(BusinessSector.PERRUQUERIA,        BusinessSize.AUTONOMO, 150,  59, 20, 20, 15, 15),
            entry(BusinessSector.PERRUQUERIA,        BusinessSize.PETIT,    300,  99, 40, 40, 20, 20),
            entry(BusinessSector.ESTETICA,           BusinessSize.AUTONOMO, 150,  59, 20, 20, 15, 15),
            entry(BusinessSector.ESTETICA,           BusinessSize.MITJA,    350, 109, 40, 40, 20, 20),
            // Serveis professionals
            entry(BusinessSector.GESTORIA,           BusinessSize.AUTONOMO, 200,  69, 20, 20, 15, 15),
            entry(BusinessSector.GESTORIA,           BusinessSize.MITJA,    400, 109, 40, 50, 25, 25),
            entry(BusinessSector.ACADEMIA,           BusinessSize.AUTONOMO, 175,  59, 20, 20, 15, 15),
            entry(BusinessSector.ACADEMIA,           BusinessSize.MITJA,    300,  99, 40, 30, 20, 20),
            // Automoció i mascotes
            entry(BusinessSector.TALLER_MECANIC,     BusinessSize.AUTONOMO, 125,  55, 20, 20, 15, 15),
            entry(BusinessSector.TALLER_MECANIC,     BusinessSize.PETIT,    200,  79, 20, 30, 15, 15),
            entry(BusinessSector.TALLER_MECANIC,     BusinessSize.MITJA,    325, 109, 30, 30, 15, 15),
            entry(BusinessSector.VETERINARI,         BusinessSize.AUTONOMO, 175,  69, 20, 20, 15, 15),
            entry(BusinessSector.VETERINARI,         BusinessSize.PETIT,    325, 109, 40, 40, 20, 20),
            entry(BusinessSector.PERRUQUERIA_CANINA, BusinessSize.AUTONOMO, 150,  59, 20, 20, 15, 15),
            // Restauració
            entry(BusinessSector.RESTAURANTE,        BusinessSize.AUTONOMO, 150,  64, 20, 20, 15, 15),
            entry(BusinessSector.RESTAURANTE,        BusinessSize.PETIT,    275, 104, 20, 30, 15, 15),
            entry(BusinessSector.RESTAURANTE,        BusinessSize.MITJA,    375, 139, 30, 30, 15, 15),
            // Immobiliaria
            entry(BusinessSector.INMOBILIARIA,       BusinessSize.AUTONOMO, 175,  79, 20, 20, 15, 15),
            entry(BusinessSector.INMOBILIARIA,       BusinessSize.PETIT,    300, 119, 30, 30, 15, 15),
            entry(BusinessSector.INMOBILIARIA,       BusinessSize.MITJA,    450, 154, 40, 40, 20, 20),
            // Agència IA / Consultoria
            entry(BusinessSector.AGENCIA_IA,         BusinessSize.AUTONOMO, 200,  89, 20, 20, 15, 15),
            entry(BusinessSector.AGENCIA_IA,         BusinessSize.PETIT,    350, 129, 30, 30, 15, 15),
            entry(BusinessSector.AGENCIA_IA,         BusinessSize.MITJA,    500, 164, 40, 40, 20, 20),
            // Atenció a la infància
            entry(BusinessSector.MARE_DE_DIA,        BusinessSize.AUTONOMO, 175,  59, 20, 20, 15, 15),
            entry(BusinessSector.MARE_DE_DIA,        BusinessSize.PETIT,    275,  89, 30, 30, 15, 15)
        ));
    }

    private SectorPricing entry(BusinessSector sector, BusinessSize size,
                                int setup, int f1, int f2, int f3, int f4, int f5) {
        // Setup per fase addicional (F2..F5): fix per mida, independent del sector
        // hores estimades × 50€/h: F2=1.5/2/2.5h, F3=1/1.5/2h, F4=1/1/1.5h, F5=1/1/1h (AUTO/PETIT/MITJA)
        int sf2 = size == BusinessSize.MITJA ? 125 : size == BusinessSize.PETIT ? 100 : 75;
        int sf3 = size == BusinessSize.MITJA ? 100 : size == BusinessSize.PETIT ? 75 : 50;
        int sf4 = size == BusinessSize.MITJA ? 75 : 50;
        int sf5 = 50;
        return SectorPricing.builder()
                .sector(sector).businessSize(size)
                .setupPrice(BigDecimal.valueOf(setup))
                .priceF1(BigDecimal.valueOf(f1))
                .priceF2(BigDecimal.valueOf(f2))
                .priceF3(BigDecimal.valueOf(f3))
                .priceF4(BigDecimal.valueOf(f4))
                .priceF5(BigDecimal.valueOf(f5))
                .setupF2(BigDecimal.valueOf(sf2))
                .setupF3(BigDecimal.valueOf(sf3))
                .setupF4(BigDecimal.valueOf(sf4))
                .setupF5(BigDecimal.valueOf(sf5))
                .build();
    }
}
