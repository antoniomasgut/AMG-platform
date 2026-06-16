package com.amg.digitalitzacio.payments.application;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodDetachParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
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
    public CheckoutResult createCheckoutSession(UUID budgetId, BigDecimal amount, String currency,
                                         String successUrl, String cancelUrl) {
        try {
            var params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .putMetadata("budgetId", budgetId.toString())
                    .putMetadata("type", "SETUP_PAYMENT")
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency.toLowerCase())
                                                    .setUnitAmount(amount.multiply(java.math.BigDecimal.valueOf(100)).longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Setup NexeLocal — Pressupost " + budgetId)
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(params);
            log.info("Stripe checkout session created: {} for budget {}", session.getId(), budgetId);
            return new CheckoutResult(session.getUrl(), session.getId());
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

    @Override
    public String createOrGetCustomer(UUID tenantId, String email) {
        try {
            var searchParams = CustomerSearchParams.builder()
                    .setQuery("metadata['tenantId']:'" + tenantId + "'")
                    .build();
            var existing = Customer.search(searchParams);
            if (!existing.getData().isEmpty()) {
                return existing.getData().get(0).getId();
            }
            var params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .putMetadata("tenantId", tenantId.toString())
                    .build();
            return Customer.create(params).getId();
        } catch (StripeException e) {
            throw new RuntimeException("Stripe customer creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String createSetupSession(String customerId, String successUrl, String cancelUrl) {
        try {
            var params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SETUP)
                    .setCustomer(customerId)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.SEPA_DEBIT)
                    .build();
            return Session.create(params).getUrl();
        } catch (StripeException e) {
            throw new RuntimeException("Stripe setup session creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public SavedPaymentMethod getPaymentMethodFromSession(String sessionId) {
        try {
            var expandParams = SessionRetrieveParams.builder()
                    .addExpand("setup_intent.payment_method")
                    .build();
            var session = Session.retrieve(sessionId, expandParams, null);
            var setupIntent = session.getSetupIntentObject();
            if (setupIntent == null) throw new RuntimeException("No setup intent in session");
            var pm = setupIntent.getPaymentMethodObject();
            if (pm == null) throw new RuntimeException("No payment method in setup intent");
            return extractPaymentMethod(pm);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to retrieve setup session: " + e.getMessage(), e);
        }
    }

    private SavedPaymentMethod extractPaymentMethod(PaymentMethod pm) {
        if (pm.getCard() != null) {
            var card = pm.getCard();
            return new SavedPaymentMethod(pm.getId(), card.getBrand(), card.getLast4(),
                    card.getExpMonth().intValue(), card.getExpYear().intValue());
        } else if (pm.getSepaDebit() != null) {
            var sepa = pm.getSepaDebit();
            return new SavedPaymentMethod(pm.getId(), "sepa_debit", sepa.getLast4(), null, null);
        }
        return new SavedPaymentMethod(pm.getId(), pm.getType(), null, null, null);
    }

    @Override
    public String chargeCustomer(String customerId, String paymentMethodId, long amountCents,
                                  String currency, String description) {
        try {
            var params = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(currency.toLowerCase())
                    .setCustomer(customerId)
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true)
                    .setOffSession(true)
                    .setDescription(description)
                    .build();
            return PaymentIntent.create(params).getId();
        } catch (StripeException e) {
            throw new RuntimeException("Stripe charge failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void detachPaymentMethod(String paymentMethodId) {
        try {
            PaymentMethod.retrieve(paymentMethodId).detach();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to detach payment method: " + e.getMessage(), e);
        }
    }
}
