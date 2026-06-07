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

    private final GoCardlessConfigRepository configRepository;
    private final VaultEncryption vaultEncryption;
    private final SystemConfigService systemConfig;
    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @Override
    public boolean isConnected() {
        try {
            var baseUrl = systemConfig.get("GOCARDLESS_API_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api-sandbox.gocardless.com";
        }
            var ping = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/ping"))
                .GET()
                .build();
            var response = http.send(ping, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("GoCardless ping failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public RedirectFlowCreated createRedirectFlow(String tenantId, String successReturnUrl, String description) {
        try {
            var config = configRepository.findByTenantId(UUID.fromString(tenantId))
                .orElseThrow(() -> new RuntimeException("GoCardless not configured for tenant " + tenantId));
            var accessToken = resolveApiKey(config);
            var baseUrl = getBaseUrl(config);

            var body = new JsonObject();
            var flows = new JsonObject();
            flows.addProperty("description", description != null ? description : "SEPA Mandate");
            flows.addProperty("session_token", tenantId);
            flows.addProperty("success_redirect_url", successReturnUrl);
            body.add("redirect_flows", flows);

            var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/redirect_flows"))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((accessToken + ":").getBytes()))
                .header("Content-Type", "application/json")
                .header("GoCardless-Version", "2015-07-06")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201 && response.statusCode() != 200) {
                throw new RuntimeException("GoCardless create redirect flow failed: " + response.statusCode() + " " + response.body());
            }

            var json = gson.fromJson(response.body(), JsonObject.class);
            var flow = json.getAsJsonObject("redirect_flows");
            var flowId = flow.get("id").getAsString();
            var redirectUrl = flow.get("redirect_url").getAsString();

            log.info("GoCardless redirect flow created: {}", flowId);
            return new RedirectFlowCreated(flowId, redirectUrl);
        } catch (Exception e) {
            throw new RuntimeException("GoCardless create redirect flow error: " + e.getMessage(), e);
        }
    }

    @Override
    public RedirectFlowResult completeRedirectFlow(String tenantId, String redirectFlowId) {
        try {
            var body = new JsonObject();
            var data = new JsonObject();
            data.addProperty("redirect_flow_id", redirectFlowId);
            body.add("data", data);

            var config = configRepository.findByTenantId(UUID.fromString(tenantId))
                .orElseThrow(() -> new RuntimeException("GoCardless not configured for tenant " + tenantId));
            var accessToken = resolveApiKey(config);
            var baseUrl = getBaseUrl(config);

            var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/redirect_flows/" + redirectFlowId + "/actions/complete"))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((accessToken + ":").getBytes()))
                .header("Content-Type", "application/json")
                .header("GoCardless-Version", "2015-07-06")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("GoCardless complete redirect flow failed: " + response.statusCode() + " " + response.body());
            }

            var json = gson.fromJson(response.body(), JsonObject.class);
            var flow = json.getAsJsonObject("redirect_flows");
            var links = flow.getAsJsonObject("links");
            var mandateId = links.get("mandate").getAsString();

            var customerBank = flow.getAsJsonObject("customer_bank_account");
            var bankName = customerBank.has("bank_name") ? customerBank.get("bank_name").getAsString() : "Unknown";
            var lastFour = customerBank.has("account_number_ending") ? customerBank.get("account_number_ending").getAsString() : "****";

            log.info("GoCardless redirect flow {} completed, mandate: {}", redirectFlowId, mandateId);
            return new RedirectFlowResult(mandateId, "GoCardless Customer", bankName, lastFour);
        } catch (Exception e) {
            throw new RuntimeException("GoCardless complete redirect flow error: " + e.getMessage(), e);
        }
    }

    @Override
    public String createPayment(String tenantId, String mandateId, BigDecimal amount, LocalDate chargeDate, String description) {
        try {
            var config = configRepository.findByTenantId(UUID.fromString(tenantId))
                .orElseThrow(() -> new RuntimeException("GoCardless not configured for tenant " + tenantId));
            var accessToken = resolveApiKey(config);
            var baseUrl = getBaseUrl(config);

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

            var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/payments"))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((accessToken + ":").getBytes()))
                .header("Content-Type", "application/json")
                .header("GoCardless-Version", "2015-07-06")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201 && response.statusCode() != 200) {
                throw new RuntimeException("GoCardless create payment failed: " + response.statusCode() + " " + response.body());
            }

            var json = gson.fromJson(response.body(), JsonObject.class);
            var paymentId = json.getAsJsonObject("payments").get("id").getAsString();
            log.info("GoCardless payment created: {} for mandate {}", paymentId, mandateId);
            return paymentId;
        } catch (Exception e) {
            throw new RuntimeException("GoCardless create payment error: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelMandate(String tenantId, String mandateId) {
        try {
            var config = configRepository.findByTenantId(UUID.fromString(tenantId))
                .orElseThrow(() -> new RuntimeException("GoCardless not configured for tenant " + tenantId));
            var accessToken = resolveApiKey(config);
            var baseUrl = getBaseUrl(config);

            var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/mandates/" + mandateId + "/actions/cancel"))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((accessToken + ":").getBytes()))
                .header("Content-Type", "application/json")
                .header("GoCardless-Version", "2015-07-06")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("GoCardless cancel mandate failed: " + response.statusCode() + " " + response.body());
            }
            log.info("GoCardless mandate cancelled: {}", mandateId);
        } catch (Exception e) {
            throw new RuntimeException("GoCardless cancel mandate error: " + e.getMessage(), e);
        }
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
        return config.getEnvironment() == GoCardlessEnvironment.LIVE
            ? "https://api.gocardless.com"
            : "https://api-sandbox.gocardless.com";
    }

}
