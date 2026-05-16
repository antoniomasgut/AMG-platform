package com.amg.digitalitzacio.payments.application;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "stripe")
@Slf4j
public class StripeRealClient implements StripeClient {

    @Value("${app.stripe.api-key:#{null}}")
    private String apiKey;

    @PostConstruct
    void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            Stripe.apiKey = apiKey;
            log.info("StripeRealClient initialized with API key");
        } else {
            log.warn("Stripe API key not configured — client will fail at runtime");
        }
    }

    @Override
    public String createCheckoutSession(UUID budgetId, BigDecimal amount, String currency,
                                         String successUrl, String cancelUrl) {
        try {
            var params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .putMetadata("budgetId", budgetId.toString())
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency.toLowerCase())
                                                    .setUnitAmount(amount.longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Pressupost " + budgetId)
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(params);
            log.info("Stripe checkout session created: {} for budget {}", session.getId(), budgetId);
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session for budget {}: {}", budgetId, e.getMessage());
            throw new RuntimeException("Stripe checkout session creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String checkPaymentStatus(String stripeSessionId) {
        try {
            Session session = Session.retrieve(stripeSessionId);
            return session.getPaymentStatus();
        } catch (StripeException e) {
            log.error("Failed to check payment status for session {}: {}", stripeSessionId, e.getMessage());
            return "unknown";
        }
    }

    @Override
    public String getReceiptUrl(String paymentIntentId) {
        try {
            var paymentIntent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
            var latestCharge = paymentIntent.getLatestCharge();
            if (latestCharge != null && !latestCharge.isBlank()) {
                var charge = com.stripe.model.Charge.retrieve(latestCharge);
                var receiptUrl = charge.getReceiptUrl();
                if (receiptUrl != null && !receiptUrl.isBlank()) {
                    return receiptUrl;
                }
            }
            log.warn("No receipt URL found for payment intent {}", paymentIntentId);
            return null;
        } catch (StripeException e) {
            log.error("Failed to retrieve receipt URL for payment intent {}: {}", paymentIntentId, e.getMessage());
            return null;
        }
    }

    @Override
    public void refundPayment(String paymentIntentId) {
        try {
            var params = com.stripe.param.RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .build();
            com.stripe.model.Refund.create(params);
            log.info("Refund created for payment intent {}", paymentIntentId);
        } catch (StripeException e) {
            log.error("Failed to refund payment intent {}: {}", paymentIntentId, e.getMessage());
            throw new RuntimeException("Stripe refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() {
        try {
            com.stripe.model.Balance.retrieve();
            return true;
        } catch (StripeException e) {
            log.warn("Stripe connection check failed: {}", e.getMessage());
            return false;
        }
    }
}
