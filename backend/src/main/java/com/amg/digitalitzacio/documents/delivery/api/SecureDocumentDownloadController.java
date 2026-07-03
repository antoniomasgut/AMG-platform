package com.amg.digitalitzacio.documents.delivery.api;

import com.amg.digitalitzacio.documents.delivery.api.dto.AcceptRequest;
import com.amg.digitalitzacio.documents.delivery.api.dto.DocumentViewResponse;
import com.amg.digitalitzacio.documents.delivery.application.DocumentViewService;
import com.amg.digitalitzacio.documents.delivery.application.SecureDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SecureDocumentDownloadController {

    private final SecureDocumentService downloadService;
    private final DocumentViewService viewService;

    /** Vista renderitzada: retorna HTML + metadades + accions disponibles. */
    @GetMapping("/api/v1/documents/view/{token}")
    public ResponseEntity<?> view(@PathVariable String token, HttpServletRequest request) {
        String ip = resolveClientIp(request);
        String ua = request.getHeader(HttpHeaders.USER_AGENT);
        DocumentViewResponse view = viewService.view(token, ip, ua);
        if (view == null) {
            return ResponseEntity.status(HttpStatus.GONE)
                .body("Aquest document ja no és accessible. Contacta amb el negoci per obtenir-ne un de nou.");
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(view);
    }

    /** Acceptació del pressupost. Retorna paymentUrl si el tenant té el cobrament online actiu (Mòdul 53). */
    @PostMapping("/api/v1/documents/view/{token}/accept")
    public ResponseEntity<?> accept(@PathVariable String token,
                                    @Valid @RequestBody AcceptRequest req,
                                    HttpServletRequest request) {
        try {
            String paymentUrl = viewService.accept(token, req, resolveClientIp(request));
            return ResponseEntity.ok(java.util.Collections.singletonMap("paymentUrl", paymentUrl));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Retorn del checkout de Stripe (Mòdul 53): verifica el pagament i redirigeix al document. */
    @GetMapping("/api/v1/documents/view/{token}/payment-return")
    public ResponseEntity<Void> paymentReturn(@PathVariable String token,
                                              @RequestParam(name = "session_id", required = false) String sessionId) {
        String redirect = viewService.handlePaymentReturn(token, sessionId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(redirect))
                .build();
    }

    /** Descàrrega del PDF. */
    @GetMapping("/api/v1/documents/download/{token}")
    public ResponseEntity<?> download(@PathVariable String token, HttpServletRequest request) {
        String ip = resolveClientIp(request);
        String ua = request.getHeader(HttpHeaders.USER_AGENT);
        var result = downloadService.validateAndStream(token, ip, ua);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.GONE)
                .body("Aquest document ja no és accessible. Contacta amb el negoci per obtenir-ne un de nou.");
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + sanitizeFileName(result.fileName()) + "\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header("X-Content-Type-Options", "nosniff")
            .contentType(parseMediaType(result.mimeType()))
            .body(new InputStreamResource(result.stream()));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\"\\\\]", "_");
    }

    private MediaType parseMediaType(String mimeType) {
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
