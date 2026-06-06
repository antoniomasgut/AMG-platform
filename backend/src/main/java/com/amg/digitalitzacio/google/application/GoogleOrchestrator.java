package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.google.api.dto.GoogleStatusResponse;
import com.amg.digitalitzacio.google.api.dto.ModuleConfigRequest;
import com.amg.digitalitzacio.google.api.dto.SendMailRequest;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfig;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfigRepository;
import com.amg.digitalitzacio.shared.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleOrchestrator {

    private final GoogleTokenService tokenService;
    private final GoogleModuleConfigRepository moduleConfigRepo;

    @Transactional(readOnly = true)
    public GoogleStatusResponse getStatus(UUID tenantId) {
        var conn = tokenService.getValidCredentials(tenantId);
        var config = moduleConfigRepo.findByTenantId(tenantId)
            .orElse(GoogleModuleConfig.defaults(tenantId));
        return new GoogleStatusResponse(true, conn.email(),
            config.isDriveEnabled(), config.isGmailEnabled(),
            config.isCalendarEnabled(), config.isSheetsEnabled());
    }

    @Transactional
    public GoogleModuleConfig updateModules(UUID tenantId, ModuleConfigRequest req) {
        var config = moduleConfigRepo.findByTenantId(tenantId)
            .orElseGet(() -> GoogleModuleConfig.defaults(tenantId));
        config.setDriveEnabled(req.driveEnabled());
        config.setGmailEnabled(req.gmailEnabled());
        config.setCalendarEnabled(req.calendarEnabled());
        config.setSheetsEnabled(req.sheetsEnabled());
        return moduleConfigRepo.save(config);
    }

    public StorageProvider getDriveProvider(UUID tenantId) {
        var creds = tokenService.getValidCredentials(tenantId);
        var config = moduleConfigRepo.findByTenantId(tenantId)
            .orElse(GoogleModuleConfig.defaults(tenantId));
        return new GoogleDriveStorageProvider(creds.accessToken(), config.getDriveFolderId());
    }

    public void sendMail(UUID tenantId, SendMailRequest req) {
        var creds = tokenService.getValidCredentials(tenantId);
        var provider = new GoogleMailProvider(creds.accessToken(), creds.email());
        provider.send(req.to(), req.subject(), req.body(), null, null, null);
    }

    public void sendMailWithAttachment(UUID tenantId, String to, String subject, String body,
                                        String attachmentName, byte[] attachmentData, String attachmentMimeType) {
        var creds = tokenService.getValidCredentials(tenantId);
        var provider = new GoogleMailProvider(creds.accessToken(), creds.email());
        provider.send(to, subject, body, attachmentName, new ByteArrayInputStream(attachmentData), attachmentMimeType);
    }

    @Transactional
    public void disconnect(UUID tenantId) {
        tokenService.revoke(tenantId);
        moduleConfigRepo.deleteById(tenantId);
    }
}
