package com.amg.digitalitzacio.engine.application;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Substitueix variables {{NOM}} als JSON de props pel valor real del tenant.
 * Permet que les plantilles continguin contingut genèric que s'adapta
 * automàticament en crear una landing per a un negoci concret.
 */
@Service
@RequiredArgsConstructor
public class TenantVariableResolver {

    private final TenantRepository tenantRepository;

    public Map<String, String> buildVariables(UUID tenantId) {
        Optional<Tenant> opt = tenantId != null ? tenantRepository.findById(tenantId) : Optional.empty();
        if (opt.isEmpty()) return Map.of();
        var t = opt.get();
        var phone = t.getContactPhone() != null ? t.getContactPhone()
                  : (t.getPhone() != null ? t.getPhone() : "");
        return Map.of(
            "BUSINESS_NAME", nvl(t.getName()),
            "PHONE",         phone,
            "CITY",          nvl(t.getCity()),
            "EMAIL",         nvl(t.getEmail()),
            "ADDRESS",       nvl(t.getAddress()),
            "SECTOR",        t.getSector() != null ? t.getSector().name() : ""
        );
    }

    /** Substitueix {{KEY}} pel valor corresponent del mapa. */
    public String resolveProps(String propsJson, Map<String, String> vars) {
        if (propsJson == null || propsJson.isBlank() || vars.isEmpty()) return propsJson;
        var result = propsJson;
        for (var entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private static String nvl(String s) { return s != null ? s : ""; }
}
