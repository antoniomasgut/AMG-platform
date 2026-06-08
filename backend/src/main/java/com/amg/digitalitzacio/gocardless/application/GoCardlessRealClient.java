package com.amg.digitalitzacio.gocardless.application;

import com.amg.digitalitzacio.gocardless.domain.GoCardlessConfig;
import com.amg.digitalitzacio.gocardless.domain.GoCardlessConfigRepository;
import com.amg.digitalitzacio.gocardless.domain.GoCardlessEnvironment;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.gocardless.provider", havingValue = "live")
@RequiredArgsConstructor
public class GoCardlessRealClient implements GoCardlessClient {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static final String GC_VERSION = "2015-07-06";
    private static final String SANDBOX_URL = "https://api-sandbox.gocardless.com";
    private static final String LIVE_URL = "https://api.gocardless.com";

    private final GoCardlessConfigRepository configRepository;
    private final VaultEncryption vaultEncryption;
    private final SystemConfigService systemConfig;

    @Override
    public boolean isConnected() {
        try {
            var baseUrl = systemConfig.get("GOCARDLESS_API_URL");
            if (baseUrl == null || baseUrl.isBlank()) baseUrl = SANDBOX_URL;
            var response = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create(baseUrl + "/ping")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("GoCardless ping failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public RedirectFlowCreated createRedirectFlow(String tenantId, String successReturnUrl, String description) {
        try {
            var config = getConfig(tenantId);
            var body = new JsonObject();
            var flows = new JsonObject();
            flows.addProperty("description", description != null ? description : "SEPA Mandate");
            flows.addProperty("session_token", tenantId);
            flows.addProperty("success_redirect_url", successReturnUrl);
            body.add("redirect_flows", flows);

            var response = HTTP.send(
                authRequest(config, getBaseUrl(config) + "/redirect_flows")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body))).build(),
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201 && response.statusCode() != 200) {
                throw new RuntimeException("GoCardless create redirect flow failed: " + response.statusCode());
            }
            var flow = GSON.fromJson(response.body(), JsonObject.class).getAsJsonObject("redirect_flows");
            log.info("GoCardless redirect flow created: {}", flow.get("id").getAsString());
            return new RedirectFlowCreated(flow.get("id").getAsString(), flow.get("redirect_url").getAsString());
        } catch (Exception e) {
            throw new RuntimeException("GoCardless create redirect flow error: " + e.getMessage(), e);
        }
    }

    @Override
    public RedirectFlowResult completeRedirectFlow(String tenantId, String redirectFlowId) {
        try {
            var config = getConfig(tenantId);
            var body = new JsonObject();
            var data = new JsonObject();
            data.addProperty("redirect_flow_id", redirectFlowId);
            body.add("data", data);

            var response = HTTP.send(
                authRequest(config, getBaseUrl(config) + "/redirect_flows/" + redirectFlowId + "/actions/complete")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body))).build(),
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("GoCardless complete redirect flow failed: " + response.statusCode());
            }
            var flow = GSON.fromJson(response.body(), JsonObject.class).getAsJsonObject("redirect_flows");
            var links = flow.getAsJsonObject("links");
            var bank = flow.getAsJsonObject("customer_bank_account");
            var mandateId = links.get("mandate").getAsString();
            var bankName = bank.has("bank_name") ? bank.get("bank_name").getAsString() : "Unknown";
            var lastFour = bank.has("account_number_ending") ? bank.get("account_number_ending").getAsString() : "****";

            log.info("GoCardless redirect flow {} completed, mandate: {}", redirectFlowId, mandateId);
            return new RedirectFlowResult(mandateId, "GoCardless Customer", bankName, lastFour);
        } catch (Exception e) {
            throw new RuntimeException("GoCardless complete redirect flow error: " + e.getMessage(), e);
        }
    }

    @Override
    public String createPayment(String tenantId, String mandateId, BigDecimal amount, LocalDate chargeDate, String description) {
        try {
            var config = getConfig(tenantId);
            var body = new JsonObject();
            var payments = new JsonObject();
            payments.addProperty("amount", amount.multiply(BigDecimal.valueOf(100)).longValue());
            payments.addProperty("currency", "EUR");
            payments.addProperty("charge_date", chargeDate.toString());
            payments.addProperty("description", description != null ? description : "AMG Monthly Invoice");
            var links = new JsonObject();
            links.addProperty("mandate", mandateId);
            payments.add("links", links);
            body.add("payments", payments);

            var response = HTTP.send(
                authRequest(config, getBaseUrl(config) + "/payments")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body))).build(),
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201 && response.statusCode() != 200) {
                throw new RuntimeException("GoCardless create payment failed: " + response.statusCode());
            }
            var paymentId = GSON.fromJson(response.body(), JsonObject.class)
                .getAsJsonObject("payments").get("id").getAsString();
            log.info("GoCardless payment created: {} for mandate {}", paymentId, mandateId);
            return paymentId;
        } catch (Exception e) {
            throw new RuntimeException("GoCardless create payment error: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelMandate(String tenantId, String mandateId) {
        try {
            var config = getConfig(tenantId);
            var response = HTTP.send(
                authRequest(config, getBaseUrl(config) + "/mandates/" + mandateId + "/actions/cancel")
                    .POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("GoCardless cancel mandate failed: " + response.statusCode());
            }
            log.info("GoCardless mandate cancelled: {}", mandateId);
        } catch (Exception e) {
            throw new RuntimeException("GoCardless cancel mandate error: " + e.getMessage(), e);
        }
    }

    private HttpRequest.Builder authRequest(GoCardlessConfig config, String url) {
        var token = resolveApiKey(config);
        var auth = "Basic " + Base64.getEncoder().encodeToString((token + ":").getBytes());
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", auth)
            .header("Content-Type", "application/json")
            .header("GoCardless-Version", GC_VERSION);
    }

    private GoCardlessConfig getConfig(String tenantId) {
        return configRepository.findByTenantId(UUID.fromString(tenantId))
            .orElseThrow(() -> new RuntimeException("GoCardless not configured for tenant " + tenantId));
    }

    private String resolveApiKey(GoCardlessConfig config) {
        if (config.getApiKeyRef() != null && !config.getApiKeyRef().isBlank()) {
            return vaultEncryption.decrypt(config.getApiKeyRef());
        }
        var envKey = config.getEnvironment() == GoCardlessEnvironment.LIVE
            ? "GOCARDLESS_LIVE_API_KEY" : "GOCARDLESS_SANDBOX_API_KEY";
        var val = systemConfig.get(envKey);
        if (val == null || val.isBlank()) {
            throw new RuntimeException("GoCardless API key not found: " + envKey);
        }
        return val;
    }

    private String getBaseUrl(GoCardlessConfig config) {
        var configuredUrl = systemConfig.get("GOCARDLESS_API_URL");
        if (configuredUrl != null && !configuredUrl.isBlank()) return configuredUrl;
        return config.getEnvironment() == GoCardlessEnvironment.LIVE ? LIVE_URL : SANDBOX_URL;
    }
}
