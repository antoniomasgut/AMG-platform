package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.ModelPricing;
import com.amg.digitalitzacio.agents.domain.ModelPricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelPricingService {

    private static final Map<String, String> MODEL_PROVIDER = Map.of(
        "claude-haiku-4-5-20251001", "anthropic",
        "claude-haiku-4-5",          "anthropic",
        "claude-sonnet-4-6",         "anthropic",
        "claude-opus-4-7",           "anthropic",
        "deepseek-chat",             "deepseek",
        "deepseek-reasoner",         "deepseek"
    );

    private final ModelPricingRepository repository;

    @Transactional(readOnly = true)
    public List<ModelPricing> getAll() {
        return repository.findAll();
    }

    /**
     * Recalcula els preus de tots els models aplicant el markup indicat.
     * Si el model ja existeix a la BD, actualitza el markup i el preu client.
     * Si no existeix, el crea.
     */
    @Transactional
    public List<ModelPricing> recalculate(int markupPercent) {
        var basePrices = TokenBudgetService.MODEL_PRICES;

        basePrices.forEach((model, costs) -> {
            var existing = repository.findById(model);
            if (existing.isPresent()) {
                existing.get().applyMarkup(markupPercent);
                repository.save(existing.get());
            } else {
                String provider = MODEL_PROVIDER.getOrDefault(model, "other");
                repository.save(ModelPricing.of(model, provider, costs[0], costs[1], markupPercent));
            }
        });

        log.info("[ModelPricing] Preus recalculats amb {}% de markup ({} models)", markupPercent, basePrices.size());
        return repository.findAll();
    }

    /** Recàlcul automàtic el primer dia de cada mes a les 00:05 UTC. */
    @Scheduled(cron = "0 5 0 1 * *")
    @Transactional
    public void scheduledRecalculate() {
        var all = repository.findAll();
        if (all.isEmpty()) {
            log.info("[ModelPricing] Cap preu configurat — omitint recàlcul mensual");
            return;
        }
        int markup = all.get(0).getMarkupPercent(); // usa el markup actual del primer model
        recalculate(markup);
        log.info("[ModelPricing] Recàlcul mensual automàtic completat (markup {}%)", markup);
    }
}
