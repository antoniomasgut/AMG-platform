package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.vault.domain.TenantCredentialRepository;
import com.amg.digitalitzacio.vault.domain.TenantServiceRepository;
import com.amg.digitalitzacio.vault.domain.VaultEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptBuilder {

    private final TenantCredentialRepository tenantCredentialRepository;
    private final TenantServiceRepository tenantServiceRepository;
    private final VaultEncryption encryption;

    private static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("business_name", "el negoci");
        DEFAULTS.put("schedule", "consulta'ns per l'horari");
        DEFAULTS.put("services", "");
        DEFAULTS.put("pricing", "");
        DEFAULTS.put("tone", "professional i proper");
        DEFAULTS.put("custom_instructions", "");
    }

    public String build(UUID tenantId) {
        String businessName = readField(tenantId, "business_name");
        String schedule = readField(tenantId, "schedule");
        String services = buildServicesString(tenantId);
        String pricing = readField(tenantId, "pricing");
        String tone = readField(tenantId, "tone");
        String customInstructions = readField(tenantId, "custom_instructions");

        return String.format("""
                Ets l'assistent virtual de %s.
                El teu to és: %s
                Respons en català de forma concisa i natural.

                HORARI: %s
                SERVEIS: %s
                PREUS: %s
                INSTRUCCIONS ESPECIALS: %s

                REGLES:
                - Si no saps alguna cosa, pregunta en lloc d'inventar
                - Confirma les dades abans de qualsevol compromís
                - En cas d'urgència o queixa greu, indica que contactin directament
                """,
                businessName,
                tone,
                schedule,
                services,
                pricing,
                customInstructions.isBlank() ? "Cap instrucció especial" : customInstructions
        );
    }

    private String readField(UUID tenantId, String fieldKey) {
        try {
            var credentials = tenantCredentialRepository.findByTenantId(tenantId);
            var fieldOpt = credentials.stream()
                    .filter(tc -> tc.getIsSet())
                    .findFirst();

            if (fieldOpt.isPresent()) {
                var decrypted = encryption.decrypt(fieldOpt.get().getEncryptedValue());
                if (decrypted != null && !decrypted.isBlank()) {
                    return decrypted;
                }
            }

            log.debug("Camp '{}' no trobat o buit per tenant {}, usant valor per defecte", fieldKey, tenantId);
            return DEFAULTS.getOrDefault(fieldKey, "");
        } catch (Exception e) {
            log.warn("Error llegint camp '{}' per tenant {}: {}. Usant valor per defecte",
                fieldKey, tenantId, e.getMessage());
            return DEFAULTS.getOrDefault(fieldKey, "");
        }
    }

    private String buildServicesString(UUID tenantId) {
        try {
            var services = tenantServiceRepository.findByTenantId(tenantId);
            if (services.isEmpty()) {
                return DEFAULTS.get("services");
            }

            var serviceNames = services.stream()
                    .map(ts -> "• Servei " + ts.getId())
                    .distinct()
                    .toList();

            return String.join("\n", serviceNames);
        } catch (Exception e) {
            log.warn("Error construint string de serveis per tenant {}: {}", tenantId, e.getMessage());
            return DEFAULTS.get("services");
        }
    }
}
