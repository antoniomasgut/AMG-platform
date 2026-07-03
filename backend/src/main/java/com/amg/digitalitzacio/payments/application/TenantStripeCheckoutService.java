package com.amg.digitalitzacio.payments.application;

import com.amg.digitalitzacio.payments.domain.StripeConfigRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Mòdul 53: Checkout d'Stripe amb la clau del TENANT (no la d'AMG).
 * Els diners van directes al compte del tenant; AMG mai toca els fons.
 *
 * Usa RequestOptions per petició — mai modifica Stripe.apiKey global,
 * que és el compte d'AMG per als setups de la plataforma.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantStripeCheckoutService {

    private final StripeConfigRepository stripeConfigRepository;
    private final VaultEncryption vaultEncryption;

    public record TenantCheckout(String url, String sessionId) {}

    /** El tenant pot cobrar online? (config activa amb clau desxifrable) */
    public boolean isConfigured(UUID tenantId) {
        return resolveApiKey(tenantId).isPresent();
    }

    /**
     * Crea una Checkout Session al compte del tenant.
     * @return empty si el tenant no té Stripe operatiu o Stripe falla — mai llança.
     */
    public Optional<TenantCheckout> createCheckout(UUID tenantId, BigDecimal amountEur,
                                                   String concept, String docTokenId,
                                                   String successUrl, String cancelUrl) {
        var apiKey = resolveApiKey(tenantId);
        if (apiKey.isEmpty()) return Optional.empty();
        if (amountEur == null || amountEur.signum() <= 0) return Optional.empty();

        try {
            var params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .putMetadata("docTokenId", docTokenId)
                    .putMetadata("tenantId", tenantId.toString())
                    .putMetadata("type", "TENANT_DOC_PAYMENT")
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("eur")
                                    .setUnitAmount(amountEur.multiply(BigDecimal.valueOf(100)).longValue())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(concept)
                                            .build())
                                    .build())
                            .build())
                    .build();

            var options = RequestOptions.builder().setApiKey(apiKey.get()).build();
            Session session = Session.create(params, options);
            log.info("[TenantStripe] Checkout {} creat per tenant {} ({} €)",
                    session.getId(), tenantId, amountEur);
            return Optional.of(new TenantCheckout(session.getUrl(), session.getId()));
        } catch (Exception e) {
            log.warn("[TenantStripe] Error creant checkout per tenant {}: {}", tenantId, e.getMessage());
            return Optional.empty();
        }
    }

    /** Verifica al retorn del checkout si la sessió està pagada (clau del tenant). */
    public boolean isSessionPaid(UUID tenantId, String sessionId) {
        var apiKey = resolveApiKey(tenantId);
        if (apiKey.isEmpty() || sessionId == null || sessionId.isBlank()) return false;
        try {
            var options = RequestOptions.builder().setApiKey(apiKey.get()).build();
            Session session = Session.retrieve(sessionId, options);
            return "paid".equalsIgnoreCase(session.getPaymentStatus());
        } catch (Exception e) {
            log.warn("[TenantStripe] Error verificant sessió {} del tenant {}: {}",
                    sessionId, tenantId, e.getMessage());
            return false;
        }
    }

    private Optional<String> resolveApiKey(UUID tenantId) {
        return stripeConfigRepository.findByTenantId(tenantId)
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .filter(c -> c.getApiKeyRef() != null && !c.getApiKeyRef().isBlank())
                .map(c -> {
                    try {
                        return vaultEncryption.decrypt(c.getApiKeyRef());
                    } catch (Exception e) {
                        log.warn("[TenantStripe] No s'ha pogut desxifrar la clau del tenant {}: {}",
                                tenantId, e.getMessage());
                        return null;
                    }
                })
                .filter(k -> k != null && !k.isBlank());
    }
}
