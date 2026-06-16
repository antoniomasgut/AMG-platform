package com.amg.digitalitzacio.documents.delivery.application;

import com.amg.digitalitzacio.documents.delivery.domain.TenantDocumentPreferences;
import com.amg.digitalitzacio.documents.delivery.domain.TenantDocumentPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantDocumentPreferencesService {

    private final TenantDocumentPreferencesRepository repo;

    @Transactional(readOnly = true)
    public TenantDocumentPreferences getPreferences(UUID tenantId) {
        return repo.findById(tenantId).orElseGet(() -> defaultPreferences(tenantId));
    }

    @Transactional
    public TenantDocumentPreferences save(UUID tenantId, String language,
                                          String standardDocChannel, String sensitiveDocChannel,
                                          Boolean autoBookingOnAccept) {
        var prefs = repo.findById(tenantId).orElseGet(() -> {
            var p = new TenantDocumentPreferences();
            p.setTenantId(tenantId);
            return p;
        });
        if (language != null && !language.isBlank()) prefs.setLanguage(language);
        if (standardDocChannel != null)  prefs.setStandardDocChannel(standardDocChannel);
        if (sensitiveDocChannel != null) prefs.setSensitiveDocChannel(sensitiveDocChannel);
        if (autoBookingOnAccept != null) prefs.setAutoBookingOnAccept(autoBookingOnAccept);
        return repo.save(prefs);
    }

    private TenantDocumentPreferences defaultPreferences(UUID tenantId) {
        var p = new TenantDocumentPreferences();
        p.setTenantId(tenantId);
        return p;
    }
}
