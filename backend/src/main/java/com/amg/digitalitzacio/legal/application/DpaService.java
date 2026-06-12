package com.amg.digitalitzacio.legal.application;

import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.legal.domain.DpaSignature;
import com.amg.digitalitzacio.legal.domain.DpaSignatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DpaService {

    private static final String DOC_VERSION = "1.0";
    private static final int EXPIRY_DAYS = 30;

    private final DpaSignatureRepository repo;
    private final TenantRepository tenantRepo;
    private final EmailService emailService;

    @Value("${app.frontend-url:https://portal.amg.cat}")
    private String frontendUrl;

    public record DpaStatusResponse(
            String status, // PENDING | SIGNED | NOT_SENT
            String signerName, String signerPosition, String signerEmail,
            String signedAt, String documentVersion, String token) {}

    public record SignRequest(String signerName, String signerPosition) {}

    @Transactional(readOnly = true)
    public DpaStatusResponse getStatus(UUID tenantId) {
        return repo.findTopByTenantIdOrderByCreatedAtDesc(tenantId).map(d -> new DpaStatusResponse(
                d.isSigned() ? "SIGNED" : (d.isExpired() ? "EXPIRED" : "PENDING"),
                d.getSignerName(), d.getSignerPosition(), d.getSignerEmail(),
                d.getSignedAt() != null ? d.getSignedAt().toString() : null,
                d.getDocumentVersion(), d.getToken()
        )).orElse(new DpaStatusResponse("NOT_SENT", null, null, null, null, null, null));
    }

    @Transactional
    public DpaStatusResponse sendRequest(UUID tenantId) {
        var tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no trobat"));

        var dpa = DpaSignature.builder()
                .tenantId(tenantId)
                .token(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 32))
                .documentVersion(DOC_VERSION)
                .expiresAt(Instant.now().plus(EXPIRY_DAYS, ChronoUnit.DAYS))
                .build();
        repo.save(dpa);

        String signingUrl = frontendUrl + "/ca/dpa/" + dpa.getToken();
        String tenantEmail = tenant.getEmail();
        String tenantName = tenant.getName();

        emailService.sendEmail(tenantEmail,
                "Acord de Tractament de Dades — AMG Digitalització",
                buildEmailText(tenantName, signingUrl));

        log.info("[DPA] Enviada sol·licitud de signatura al tenant {} ({})", tenantName, tenantEmail);
        return getStatus(tenantId);
    }

    @Transactional(readOnly = true)
    public DpaPreviewResponse getDocument(String token) {
        var dpa = repo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invàlid"));
        if (dpa.isExpired() && !dpa.isSigned())
            throw new IllegalStateException("L'enllaç ha caducat. Demana un nou enviament.");

        var tenant = tenantRepo.findById(dpa.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant no trobat"));

        return new DpaPreviewResponse(
                dpa.getToken(), dpa.getDocumentVersion(),
                dpa.isSigned(), dpa.isSigned() ? dpa.getSignedAt().toString() : null,
                tenant.getName(), tenant.getEmail(),
                dpa.getSignerName(), dpa.getSignerPosition()
        );
    }

    @Transactional
    public void sign(String token, SignRequest req, String ipAddress, String userAgent) {
        var dpa = repo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invàlid"));
        if (dpa.isSigned())
            throw new IllegalStateException("Aquest document ja ha estat signat.");
        if (dpa.isExpired())
            throw new IllegalStateException("L'enllaç ha caducat.");
        if (req.signerName() == null || req.signerName().isBlank())
            throw new IllegalArgumentException("El nom del signant és obligatori.");

        var tenant = tenantRepo.findById(dpa.getTenantId()).orElseThrow();

        dpa.setSignerName(req.signerName().trim());
        dpa.setSignerPosition(req.signerPosition() != null ? req.signerPosition().trim() : null);
        dpa.setSignerEmail(tenant.getEmail());
        dpa.setSignedAt(Instant.now());
        dpa.setIpAddress(ipAddress);
        dpa.setUserAgent(userAgent);
        repo.save(dpa);

        emailService.sendEmail(tenant.getEmail(),
                "Confirmat — Acord de Tractament de Dades signat",
                buildConfirmationText(tenant.getName(), req.signerName(), dpa.getSignedAt().toString()));

        log.info("[DPA] Signat per {} ({}) — tenant {} — IP {}",
                req.signerName(), tenant.getEmail(), tenant.getName(), ipAddress);
    }

    public record DpaPreviewResponse(
            String token, String documentVersion,
            boolean signed, String signedAt,
            String tenantName, String tenantEmail,
            String signerName, String signerPosition) {}

    private String buildEmailText(String tenantName, String signingUrl) {
        return """
                Benvolgut/da %s,

                AMG Digitalització us envia per a la vostra signatura l'Acord de Tractament de Dades (ATD) que regula com gestionem les dades dels vostres clients en el marc dels serveis contractats.

                Podeu llegir i signar el document a:
                %s

                L'enllaç caduca en %d dies.

                Si teniu qualsevol dubte, contacteu amb nosaltres a hola@amgdl.com.

                Gràcies,
                AMG Digitalització
                """.formatted(tenantName, signingUrl, EXPIRY_DAYS);
    }

    private String buildConfirmationText(String tenantName, String signerName, String signedAt) {
        return """
                Benvolgut/da %s,

                Confirmem la recepció de la signatura de l'Acord de Tractament de Dades (ATD) per part de %s el %s.

                Podeu sol·licitar una còpia del document signat en qualsevol moment contactant a hola@amgdl.com.

                Gràcies,
                AMG Digitalització
                """.formatted(tenantName, signerName, signedAt);
    }
}
