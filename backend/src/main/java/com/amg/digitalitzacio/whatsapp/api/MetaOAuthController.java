package com.amg.digitalitzacio.whatsapp.api;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.whatsapp.api.dto.MetaConfirmRequest;
import com.amg.digitalitzacio.whatsapp.api.dto.MetaConnectUrlResponse;
import com.amg.digitalitzacio.whatsapp.api.dto.MetaOAuthSessionResponse;
import com.amg.digitalitzacio.whatsapp.application.MetaOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MetaOAuthController {

    private final MetaOAuthService    metaOAuthService;
    private final SystemConfigService sysConfig;

    /**
     * Pas 1: el tenant (ADMIN) sol·licita l'URL OAuth per iniciar la connexió.
     * Retorna l'URL on s'ha de redirigir el navegador.
     */
    @GetMapping("/api/v1/meta/whatsapp/connect")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public MetaConnectUrlResponse startConnect(@RequestParam UUID tenantId) {
        String url = metaOAuthService.generateConnectUrl(tenantId);
        return new MetaConnectUrlResponse(url);
    }

    /**
     * Pas 2: Meta redirigeix aquí amb el codi OAuth.
     * Públic — és una redirecció de navegador, no una crida autenticada.
     * Redirigeix al frontend amb un sessionToken per continuar el procés.
     */
    @GetMapping("/oauth/meta/whatsapp/callback")
    public ResponseEntity<Void> oauthCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        String frontendBase = frontendUrl();

        if (error != null || code == null || state == null) {
            log.warn("Meta OAuth error: {} — {}", error, errorDescription);
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendBase + "/portal/admin/integrations/whatsapp?error=" + encode(errorDescription != null ? errorDescription : "cancel·lat")))
                .build();
        }

        try {
            String sessionToken = metaOAuthService.handleCallback(code, state);
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendBase + "/portal/admin/integrations/whatsapp?session=" + sessionToken))
                .build();
        } catch (Exception e) {
            log.error("Error processant callback Meta OAuth: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendBase + "/portal/admin/integrations/whatsapp?error=" + encode(e.getMessage())))
                .build();
        }
    }

    /**
     * Pas 3: el frontend llegeix el resultat de la sessió OAuth per mostrar
     * la llista de WABAs i números al tenant.
     * Públic — el sessionToken actua com a credencial de curta durada (5 min).
     */
    @GetMapping("/api/v1/meta/whatsapp/oauth-session/{sessionToken}")
    public MetaOAuthSessionResponse getOAuthSession(@PathVariable String sessionToken) {
        return metaOAuthService.getSession(sessionToken);
    }

    /**
     * Pas 4: el tenant confirma la selecció de WABA + número.
     * El backend guarda la configuració i subscriu el webhook.
     */
    @PostMapping("/api/v1/meta/whatsapp/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void confirmConnection(@RequestBody MetaConfirmRequest req) {
        metaOAuthService.confirmConnection(req.sessionToken(), req.wabaId(), req.phoneNumberId());
    }

    /**
     * Desconnecta: revoca els permisos Meta i elimina la configuració.
     */
    @DeleteMapping("/api/v1/meta/whatsapp/disconnect/{tenantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void disconnect(@PathVariable UUID tenantId) {
        metaOAuthService.disconnect(tenantId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String frontendUrl() {
        String url = sysConfig.get("FRONTEND_URL");
        return (url != null && !url.isBlank()) ? url : "https://amgdl.com";
    }

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
