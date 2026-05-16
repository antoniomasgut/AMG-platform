package com.amg.digitalitzacio.finops.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.finops.provider", havingValue = "holded")
@Slf4j
public class FinOpsHoldedClient implements FinOpsClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public FinOpsHoldedClient(
            @Value("${app.finops.holded.api-url}") String apiUrl,
            @Value("${app.finops.holded.api-key:#{null}}") String apiKey,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        var builder = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("key", apiKey);
        } else {
            log.warn("Holded API key not configured — client will fail at runtime");
        }

        this.webClient = builder.build();
    }

    @Override
    public String createContact(String tenantName, String email, String phone, String nif) {
        try {
            var body = Map.of(
                    "name", tenantName != null ? tenantName : "Tenant " + System.currentTimeMillis(),
                    "email", email != null ? email : "",
                    "phone", phone != null ? phone : "",
                    "nif", nif != null ? nif : ""
            );

            var response = webClient.post()
                    .uri("/contacts")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            var contactId = response != null ? response.path("id").asText() : null;
            log.info("Holded contact created: {} for tenant {}", contactId, tenantName);
            return contactId;
        } catch (Exception e) {
            log.error("Failed to create Holded contact: {}", e.getMessage());
            throw new RuntimeException("Holded contact creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateContact(String holdedContactId, String tenantName, String email, String phone) {
        try {
            var body = Map.of(
                    "name", tenantName != null ? tenantName : "",
                    "email", email != null ? email : "",
                    "phone", phone != null ? phone : ""
            );

            webClient.put()
                    .uri("/contacts/{id}", holdedContactId)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Holded contact updated: {}", holdedContactId);
        } catch (Exception e) {
            log.error("Failed to update Holded contact {}: {}", holdedContactId, e.getMessage());
            throw new RuntimeException("Holded contact update failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean contactExists(String holdedContactId) {
        try {
            var response = webClient.get()
                    .uri("/contacts/{id}", holdedContactId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return response != null && response.has("id");
        } catch (Exception e) {
            log.warn("Holded contact check failed for {}: {}", holdedContactId, e.getMessage());
            return false;
        }
    }

    @Override
    public String createInvoice(String holdedContactId, BigDecimal amount, BigDecimal taxAmount,
                                String description, String dueDate) {
        try {
            var body = objectMapper.createObjectNode();
            body.put("contact", holdedContactId)
                    .put("desc", description != null ? description : "")
                    .put("date", java.time.LocalDate.now().toString())
                    .set("items", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .put("name", description != null ? description : "Servei AMG")
                                    .put("units", 1)
                                    .put("subtotal", amount != null ? amount.doubleValue() : 0)
                                    .put("tax", taxAmount != null ? taxAmount.doubleValue() : 0)));

            if (dueDate != null) {
                body.put("duedate", dueDate);
            }

            var response = webClient.post()
                    .uri("/invoices")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            var invoiceId = response != null ? response.path("id").asText() : null;
            log.info("Holded invoice created: {} for contact {}", invoiceId, holdedContactId);
            return invoiceId;
        } catch (Exception e) {
            log.error("Failed to create Holded invoice: {}", e.getMessage());
            throw new RuntimeException("Holded invoice creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getInvoiceStatus(String holdedInvoiceId) {
        try {
            var response = webClient.get()
                    .uri("/invoices/{id}", holdedInvoiceId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return response != null ? response.path("status").asText("unknown") : "unknown";
        } catch (Exception e) {
            log.error("Failed to get Holded invoice status for {}: {}", holdedInvoiceId, e.getMessage());
            return "unknown";
        }
    }

    @Override
    public String getInvoicePdfUrl(String holdedInvoiceId) {
        try {
            var response = webClient.get()
                    .uri("/invoices/{id}/pdf", holdedInvoiceId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            var pdfUrl = response != null ? response.path("url").asText() : null;
            if (pdfUrl == null || pdfUrl.isBlank()) {
                pdfUrl = "https://app.holded.com/invoices/" + holdedInvoiceId + "/pdf";
            }
            return pdfUrl;
        } catch (Exception e) {
            log.warn("Failed to get Holded invoice PDF URL for {}: {}", holdedInvoiceId, e.getMessage());
            return null;
        }
    }

    @Override
    public void cancelInvoice(String holdedInvoiceId) {
        try {
            webClient.delete()
                    .uri("/invoices/{id}", holdedInvoiceId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Holded invoice cancelled: {}", holdedInvoiceId);
        } catch (Exception e) {
            log.error("Failed to cancel Holded invoice {}: {}", holdedInvoiceId, e.getMessage());
            throw new RuntimeException("Holded invoice cancellation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() {
        try {
            var response = webClient.get()
                    .uri("/contacts?limit=1")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return response != null;
        } catch (Exception e) {
            log.warn("Holded connection check failed: {}", e.getMessage());
            return false;
        }
    }
}
