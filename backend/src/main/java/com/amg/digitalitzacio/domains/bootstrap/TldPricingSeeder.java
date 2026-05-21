package com.amg.digitalitzacio.domains.bootstrap;

import com.amg.digitalitzacio.domains.domain.TldPricing;
import com.amg.digitalitzacio.domains.domain.TldPricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Seeder de preus per TLD — s'executa a l'arrencada si la taula està buida
@Slf4j
@Component
@RequiredArgsConstructor
public class TldPricingSeeder implements CommandLineRunner {

    private final TldPricingRepository tldPricingRepository;

    @Override
    public void run(String... args) {
        // Ometre si ja hi ha dades a la taula
        if (tldPricingRepository.count() > 0) {
            log.info("TldPricingSeeder: taula ja poblada, ometent seeder");
            return;
        }

        log.info("TldPricingSeeder: iniciant càrrega de preus per TLD...");

        tldPricingRepository.save(TldPricing.builder()
                .tld("cat")
                .costRegister(BigDecimal.valueOf(11))
                .costRenew(BigDecimal.valueOf(11))
                .saleRegister(BigDecimal.valueOf(20))
                .saleRenew(BigDecimal.valueOf(18))
                .isActive(true)
                .build());

        tldPricingRepository.save(TldPricing.builder()
                .tld("es")
                .costRegister(BigDecimal.valueOf(4))
                .costRenew(BigDecimal.valueOf(4))
                .saleRegister(BigDecimal.valueOf(12))
                .saleRenew(BigDecimal.valueOf(10))
                .isActive(true)
                .build());

        tldPricingRepository.save(TldPricing.builder()
                .tld("com")
                .costRegister(BigDecimal.valueOf(8))
                .costRenew(BigDecimal.valueOf(8))
                .saleRegister(BigDecimal.valueOf(15))
                .saleRenew(BigDecimal.valueOf(13))
                .isActive(true)
                .build());

        tldPricingRepository.save(TldPricing.builder()
                .tld("eu")
                .costRegister(BigDecimal.valueOf(5))
                .costRenew(BigDecimal.valueOf(5))
                .saleRegister(BigDecimal.valueOf(12))
                .saleRenew(BigDecimal.valueOf(10))
                .isActive(true)
                .build());

        tldPricingRepository.save(TldPricing.builder()
                .tld("net")
                .costRegister(BigDecimal.valueOf(9))
                .costRenew(BigDecimal.valueOf(9))
                .saleRegister(BigDecimal.valueOf(15))
                .saleRenew(BigDecimal.valueOf(13))
                .isActive(true)
                .build());

        log.info("TldPricingSeeder: 5 TLDs carregats (cat, es, com, eu, net)");
    }
}
