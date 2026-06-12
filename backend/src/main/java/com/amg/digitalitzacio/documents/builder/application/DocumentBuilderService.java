package com.amg.digitalitzacio.documents.builder.application;

import com.amg.digitalitzacio.documents.builder.api.dto.*;
import com.amg.digitalitzacio.documents.builder.domain.*;
import com.amg.digitalitzacio.google.application.GoogleOrchestrator;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.shared.storage.StorageProviderRouter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentBuilderService {

    private static final Map<String, String> DOC_PREFIXES = Map.of(
        "invoice",       "F",
        "quote",         "P",
        "delivery_note", "A",
        "contract",      "C",
        "report",        "I",
        "proposal",      "PR",
        "image_release", "CD",
        "custom",        "DOC"
    );

    private final DocumentTemplateRepository templateRepo;
    private final DocumentTemplateVersionRepository versionRepo;
    private final GeneratedDocumentRepository documentRepo;
    private final DocumentNumberSequenceRepository seqRepo;
    private final ObjectMapper objectMapper;
    private final StorageProviderRouter storageRouter;
    private final GoogleOrchestrator googleOrchestrator;
    private final AIProviderRouter aiProviderRouter;

    public List<TemplateResponse> listTemplates(UUID tenantId) {
        return templateRepo.findByTenantIdOrderByUpdatedAtDesc(tenantId).stream()
            .map(this::toTemplateResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public TemplateResponse createTemplate(UUID tenantId, TemplateRequest req) {
        var t = new DocumentTemplate();
        t.setTenantId(tenantId);
        t.setName(req.name());
        t.setDocumentType(req.documentType());
        t.setLayout(req.layout() != null ? req.layout() : "[]");
        t.setDataBindings(req.dataBindings() != null ? req.dataBindings() : "{}");
        t.setStyles(req.styles() != null ? req.styles() : "{}");
        var saved = templateRepo.save(t);

        saveVersionSnapshot(saved, "Versió inicial");
        return toTemplateResponse(saved);
    }

    public TemplateResponse getTemplate(UUID id) {
        var t = templateRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Template not found: " + id));
        return toTemplateResponse(t);
    }

    @Transactional
    public TemplateResponse updateTemplate(UUID id, TemplateRequest req, UserPrincipal user) {
        var t = templateRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Template not found: " + id));
        assertOwner(t, user);
        if (req.name() != null) t.setName(req.name());
        if (req.layout() != null) t.setLayout(req.layout());
        if (req.dataBindings() != null) t.setDataBindings(req.dataBindings());
        if (req.styles() != null) t.setStyles(req.styles());
        t.setVersion(t.getVersion() + 1);
        var saved = templateRepo.save(t);

        saveVersionSnapshot(saved, "Actualització via API");
        return toTemplateResponse(saved);
    }

    @Transactional
    public void deleteTemplate(UUID id, UserPrincipal user) {
        var t = templateRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Template not found: " + id));
        assertOwner(t, user);
        t.setActive(false);
        templateRepo.save(t);
    }

    @Transactional
    public TemplateResponse duplicateTemplate(UUID id, UserPrincipal user) {
        var t = templateRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Template not found: " + id));
        assertOwner(t, user);
        var dup = new DocumentTemplate();
        dup.setTenantId(user.tenantId() != null ? user.tenantId() : t.getTenantId());
        dup.setName(t.getName() + " (còpia)");
        dup.setDocumentType(t.getDocumentType());
        dup.setLayout(t.getLayout());
        dup.setDataBindings(t.getDataBindings());
        dup.setStyles(t.getStyles());
        var saved = templateRepo.save(dup);
        saveVersionSnapshot(saved, "Duplicat de " + t.getName());
        return toTemplateResponse(saved);
    }

    public List<VersionResponse> listVersions(UUID templateId) {
        return versionRepo.findByTemplateIdOrderByVersionDesc(templateId).stream()
            .map(v -> new VersionResponse(v.getId(), v.getTemplateId(), v.getVersion(),
                v.getLayout(), v.getDataBindings(), v.getStyles(), v.getNotes(), v.getCreatedAt()))
            .collect(Collectors.toList());
    }

    public VersionResponse getVersion(UUID templateId, Integer version) {
        var v = versionRepo.findByTemplateIdAndVersion(templateId, version)
            .orElseThrow(() -> new NoSuchElementException("Version not found"));
        return new VersionResponse(v.getId(), v.getTemplateId(), v.getVersion(),
            v.getLayout(), v.getDataBindings(), v.getStyles(), v.getNotes(), v.getCreatedAt());
    }

    @Transactional
    public TemplateResponse restoreVersion(UUID templateId, Integer version, UserPrincipal user) {
        var v = versionRepo.findByTemplateIdAndVersion(templateId, version)
            .orElseThrow(() -> new NoSuchElementException("Version not found"));
        var t = templateRepo.findById(templateId)
            .orElseThrow(() -> new NoSuchElementException("Template not found"));
        assertOwner(t, user);
        t.setLayout(v.getLayout());
        t.setDataBindings(v.getDataBindings());
        t.setStyles(v.getStyles());
        t.setVersion(t.getVersion() + 1);
        var saved = templateRepo.save(t);
        saveVersionSnapshot(saved, "Restaurat versió " + version);
        return toTemplateResponse(saved);
    }

    public String previewTemplate(UUID id) {
        var t = templateRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Template not found: " + id));
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("company", Map.of(
            "name", "Nom Empresa", "taxId", "B12345678",
            "phone", "+34 612 345 678", "email", "info@empresa.com",
            "address", "C/ Exemple 1, 07001 Palma"
        ));
        ctx.put("customer", Map.of(
            "name", "Client Exemple", "taxId", "12345678A",
            "address", "C/ Major 10, Palma", "phone", "+34 600 000 000", "email", "client@exemple.com"
        ));
        ctx.put("document", Map.of(
            "number", "DOC-0001", "date", LocalDate.now().toString(),
            "validUntil", LocalDate.now().plusDays(30).toString()
        ));
        return renderHtml(t.getLayout(), ctx, t.getStyles());
    }

    @Transactional
    public DocumentResponse generateDocument(UUID tenantId, GenerateRequest req) {
        var t = templateRepo.findById(req.templateId())
            .orElseThrow(() -> new NoSuchElementException("Template not found"));

        String docTypeStr = t.getDocumentType() != null ? t.getDocumentType().name() : "custom";
        String number = nextDocNumber(tenantId, docTypeStr);

        Map<String, Object> variables = req.variables() != null ? req.variables() : new HashMap<>();
        List<GenerateRequest.ArticleLine> articles = req.articles() != null ? req.articles() : List.of();

        Map<String, Object> ctx = buildContext(tenantId, req, number, variables, articles);

        String html = renderHtml(t.getLayout(), ctx, t.getStyles());

        var doc = new GeneratedDocument();
        doc.setTenantId(tenantId);
        doc.setTemplateId(req.templateId());
        doc.setTemplateVersion(t.getVersion());
        doc.setNumber(number);
        doc.setCustomerId(req.customerId());
        doc.setCustomerData(toJson(req.customerData()));
        doc.setVariables(toJson(variables));
        doc.setArticles(toJson(articles));
        doc.setCalculated(toJson(ctx.get("calculated")));
        doc.setHtmlContent(html);
        var saved = documentRepo.save(doc);

        return toDocumentResponse(saved);
    }

    @Transactional
    public DocumentResponse generatePdf(UUID tenantId, GenerateRequest req) {
        var doc = generateDocument(tenantId, req);
        var entity = documentRepo.findById(doc.id()).orElseThrow();

        try {
            var html = entity.getHtmlContent();
            if (html != null) {
                var pdfBytes = renderPdf(html);
                var provider = storageRouter.getProvider(tenantId);
                var result = provider.upload(
                    new ByteArrayInputStream(pdfBytes),
                    doc.number() + ".pdf", "application/pdf");
                entity.setStorageProvider(result.provider());
                entity.setStorageFileId(result.fileId());
                entity.setStoragePath(result.fileName());
                var signedUrl = provider.getSignedUrl(result.fileId(), Duration.ofDays(1));
                entity.setPdfUrl(signedUrl != null ? signedUrl : doc.number() + ".pdf");
                log.info("PDF stored via {} for doc {}", result.provider(), doc.number());
            } else {
                entity.setPdfUrl("/api/v1/documents/" + doc.id() + "/pdf");
            }
        } catch (Exception e) {
            log.warn("Failed to store PDF externally for {}: {}", doc.number(), e.getMessage());
            entity.setPdfUrl("/api/v1/documents/" + doc.id() + "/pdf");
        }

        documentRepo.save(entity);
        return toDocumentResponse(entity);
    }

    private byte[] renderPdf(String html) throws Exception {
        try {
            var os = new ByteArrayOutputStream();
            var renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        } catch (Exception e) {
            log.error("PDF rendering failed — {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    public List<DocumentResponse> listDocuments(UUID tenantId) {
        return documentRepo.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
            .map(this::toDocumentResponse)
            .collect(Collectors.toList());
    }

    public DocumentResponse getDocument(UUID id) {
        var d = documentRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Document not found: " + id));
        return toDocumentResponse(d);
    }

    public AIApplyRequest.AIApplyResponse applyAiOperations(UUID templateId, String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }
        var operations = new ArrayList<AIApplyRequest.AIOperation>();

        if (prompt.toLowerCase().contains("logo") && prompt.toLowerCase().contains("esquerra")) {
            operations.add(new AIApplyRequest.AIOperation("move", "logo", 0, 0, null, null, null));
        }
        if (prompt.toLowerCase().contains("signatura") || prompt.toLowerCase().contains("signature")) {
            Map<String, Object> style = new HashMap<>();
            style.put("alignment", "right");
            operations.add(new AIApplyRequest.AIOperation("add_block", "signature", null, null, 6, 2, style));
        }
        if (prompt.toLowerCase().contains("títol") && prompt.toLowerCase().contains("blau")) {
            operations.add(new AIApplyRequest.AIOperation("update_style", "document_title", null, null, null, null,
                Map.of("fontSize", 18, "fontWeight", "bold", "color", "#1a237e")));
        }

        return new AIApplyRequest.AIApplyResponse(operations);
    }

    private Map<String, Object> buildContext(UUID tenantId, GenerateRequest req, String number,
                                              Map<String, Object> variables, List<GenerateRequest.ArticleLine> articles) {
        Map<String, Object> ctx = new HashMap<>();

        ctx.put("company", Map.of(
            "name", "Empresa", "taxId", "", "phone", "",
            "email", "", "address", "", "logo", ""
        ));

        if (req.customerData() != null) {
            ctx.put("customer", req.customerData());
        } else {
            ctx.put("customer", Map.of(
                "name", "", "taxId", "", "address", "", "phone", "", "email", ""
            ));
        }

        ctx.put("document", Map.of(
            "number", number,
            "date", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            "validUntil", LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        ));

        ctx.put("variables", variables);

        var calculated = calculate(articles, variables);
        ctx.put("calculated", calculated);

        return ctx;
    }

    private Map<String, Object> calculate(List<GenerateRequest.ArticleLine> articles, Map<String, Object> variables) {
        double subtotal = 0;
        var lines = new ArrayList<Map<String, Object>>();
        for (var a : articles) {
            double total = a.quantity() * a.unitPrice();
            lines.add(Map.of(
                "description", a.description(),
                "quantity", a.quantity(),
                "unitPrice", a.unitPrice(),
                "total", total
            ));
            subtotal += total;
        }

        double discount = 0;
        if (variables.containsKey("hours")) {
            Object hoursObj = variables.get("hours");
            double hoursVal = 0;
            if (hoursObj instanceof Number n) {
                hoursVal = n.doubleValue();
            } else if (hoursObj != null) {
                try { hoursVal = Double.parseDouble(hoursObj.toString()); } catch (NumberFormatException ignore) {}
            }
            if (hoursVal > 20) discount = 10;
        }

        double taxRate = 21;
        double tax = (subtotal - discount) * taxRate / 100;
        double total = subtotal - discount + tax;

        var fmt = NumberFormat.getCurrencyInstance(Locale.of("es", "ES"));
        return Map.of(
            "lines", lines,
            "subtotal", fmt.format(subtotal),
            "subtotalRaw", subtotal,
            "discount", fmt.format(discount),
            "discountRaw", discount,
            "taxRate", taxRate,
            "tax", fmt.format(tax),
            "taxRaw", tax,
            "total", fmt.format(total),
            "totalRaw", total
        );
    }

    private String nextDocNumber(UUID tenantId, String docType) {
        int year = LocalDate.now().getYear();
        String prefix = DOC_PREFIXES.getOrDefault(docType, "DOC");
        seqRepo.initIfAbsent(tenantId, docType, year);
        var seq = seqRepo.findForUpdate(tenantId, docType, year).orElseThrow();
        String number = prefix + "-" + year + "-" + String.format("%03d", seq.getNextNumber());
        seq.setNextNumber(seq.getNextNumber() + 1);
        seqRepo.save(seq);
        return number;
    }

    private void assertOwner(DocumentTemplate t, UserPrincipal user) {
        if (!"SUPER_ADMIN".equals(user.role()) && !t.getTenantId().equals(user.tenantId())) {
            throw new SecurityException("Access denied to template " + t.getId());
        }
    }

    private String renderHtml(String layoutJson, Map<String, Object> ctx, String stylesJson) {
        try {
            var layout = objectMapper.readTree(layoutJson);
            var styles = objectMapper.readTree(stylesJson);

            StringBuilder sb = new StringBuilder();
            sb.append("""
                <!DOCTYPE html><html><head><meta charset="UTF-8">
                <style>
                  @page { margin: 0; }
                  * { box-sizing: border-box; }
                  body { font-family: 'Helvetica Neue', Arial, sans-serif; margin: 48px 56px; color: #1a1a1a; font-size: 13px; line-height: 1.5; background: #fff; }
                  table { width: 100%%; border-collapse: collapse; margin: 0; }
                  th { padding: 8px 12px; text-align: left; background: #f0f0f0; font-weight: 600; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: #555; border-bottom: 2px solid #ddd; }
                  td { padding: 9px 12px; text-align: left; border-bottom: 1px solid #ebebeb; color: #333; }
                  tr:last-child td { border-bottom: none; }
                  .text-right { text-align: right !important; }
                  .text-center { text-align: center !important; }
                  .total-row td { font-weight: 700; font-size: 15px; border-top: 2px solid #1a1a1a; border-bottom: none; padding-top: 12px; }
                  .subtotal-section { margin-top: 16px; }
                  .subtotal-section p { margin: 4px 0; text-align: right; }
                  .header-accent { border-bottom: 3px solid #FF6B00; padding-bottom: 20px; margin-bottom: 24px; }
                  .section-label { font-size: 10px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; color: #888; margin-bottom: 4px; }
                  .footer { border-top: 1px solid #ddd; padding-top: 16px; margin-top: 32px; font-size: 11px; color: #888; line-height: 1.6; }
                  hr { border: none; border-top: 1px solid #e5e5e5; margin: 20px 0; }
                  h1 { font-size: 26px; font-weight: 700; letter-spacing: 3px; text-transform: uppercase; color: #1a1a1a; margin: 0; }
                  h2 { font-size: 16px; font-weight: 700; color: #1a1a1a; margin: 0 0 4px 0; }
                  h3 { font-size: 13px; font-weight: 600; margin: 0 0 6px 0; color: #1a1a1a; }
                  p { margin: 2px 0; }
                  .accent { color: #FF6B00; }
                  .totals-table { width: auto; margin-left: auto; min-width: 260px; }
                  .totals-table td { padding: 5px 12px; border: none; font-size: 13px; }
                  .totals-table .label { color: #666; }
                  .totals-table .value { text-align: right; font-weight: 600; }
                  .totals-table .grand-total td { font-size: 16px; font-weight: 700; border-top: 2px solid #1a1a1a; padding-top: 10px; }
                  .sig-block { margin-top: 48px; display: inline-block; min-width: 200px; }
                  .sig-line { border-top: 1px solid #999; padding-top: 6px; font-size: 11px; color: #888; }
                </style>
                </head><body>
                """);

            if (layout.isArray()) {
                for (var block : layout) {
                    sb.append(renderBlock(block, ctx));
                }
            }

            sb.append("</body></html>");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Error rendering HTML: {}", e.getMessage());
            return "<html><body><p>Error rendering template</p></body></html>";
        }
    }

    private String renderBlock(com.fasterxml.jackson.databind.JsonNode block, Map<String, Object> ctx) {
        String type = block.path("type").asText();
        var config = block.path("config");
        var dataBinding = block.path("dataBinding");
        String source = dataBinding.path("source").asText();
        String field = dataBinding.path("field").asText();

        String alignment = config.path("style").path("alignment").asText("left");
        String alignClass = switch (alignment) {
            case "center" -> "text-center";
            case "right" -> "text-right";
            default -> "";
        };

        return switch (type) {
            case "document_title" -> "<h1 class=\"" + alignClass + "\">" + resolvePlaceholder(config, "text", "DOCUMENT") + "</h1>";
            case "document_number" -> "<p class=\"" + alignClass + "\"><strong>Número:</strong> " + resolveCtx(ctx, "document", "number") + "</p>";
            case "document_date" -> "<p class=\"" + alignClass + "\"><strong>Data:</strong> " + resolveCtx(ctx, "document", "date") + "</p>";
            case "document_validity" -> "<p class=\"" + alignClass + "\"><strong>Validesa:</strong> " + resolveCtx(ctx, "document", "validUntil") + "</p>";
            case "company_info" -> renderCompanyInfo(ctx, alignClass);
            case "customer_info" -> renderCustomerInfo(ctx, alignClass);
            case "products_table", "services_table" -> renderTable(ctx);
            case "subtotal" -> "<p class=\"" + alignClass + "\"><strong>Subtotal:</strong> " + resolveCtx(ctx, "calculated", "subtotal") + "</p>";
            case "discount" -> "<p class=\"" + alignClass + "\"><strong>Descompte:</strong> " + resolveCtx(ctx, "calculated", "discount") + "</p>";
            case "tax" -> "<p class=\"" + alignClass + "\"><strong>IVA (" + resolveCtx(ctx, "calculated", "taxRate") + "%):</strong> " + resolveCtx(ctx, "calculated", "tax") + "</p>";
            case "total" -> "<p class=\"" + alignClass + " total-row\"><strong>Total:</strong> " + resolveCtx(ctx, "calculated", "total") + "</p>";
            case "text" -> "<p class=\"" + alignClass + "\">" + resolvePlaceholder(config, "text", "") + "</p>";
            case "rich_text" -> "<div class=\"" + alignClass + "\">" + resolveHtml(ctx, config.path("html").asText("")) + "</div>";
            case "separator" -> "<hr>";
            case "terms" -> "<div class=\"footer\"><p><strong>Termes i condicions:</strong> " + resolvePlaceholder(config, "text", "") + "</p></div>";
            case "conditions" -> "<div class=\"footer\"><p><strong>Condicions generals:</strong> " + resolvePlaceholder(config, "text", "") + "</p></div>";
            case "signature" -> "<div class=\"" + alignClass + "\" style=\"margin-top: 40px;\"><p>_________________________</p><p>Signatura</p></div>";
            case "logo" -> renderLogo(ctx, config, alignClass);
            case "image" -> renderImage(config, alignClass);
            case "page_break" -> "<div style=\"page-break-before: always;\"></div>";
            case "qr_code" -> renderPlaceholderBlock(alignClass, "QR" + (config.path("value").asText("").isBlank() ? "" : ": " + esc(config.path("value").asText(""))));
            case "barcode" -> renderPlaceholderBlock(alignClass, "Codi barres" + (config.path("value").asText("").isBlank() ? "" : ": " + esc(config.path("value").asText(""))));
            case "payment_link" -> "<p class=\"" + alignClass + "\"><a href=\"" + esc(config.path("url").asText("#")) + "\" style=\"color: #FF6B00; text-decoration: underline;\">" + esc(config.path("text").asText("Pagar en línia")) + "</a></p>";
            default -> "";
        };
    }

    private String renderCompanyInfo(Map<String, Object> ctx, String alignClass) {
        @SuppressWarnings("unchecked")
        var c = (Map<String, Object>) ctx.getOrDefault("company", Map.of());
        return "<div class=\"" + alignClass + " header\">" +
            "<h2>" + esc(c.getOrDefault("name", "").toString()) + "</h2>" +
            "<p>" + esc(c.getOrDefault("address", "").toString()) + "</p>" +
            "<p>Tel: " + esc(c.getOrDefault("phone", "").toString()) + " | Email: " + esc(c.getOrDefault("email", "").toString()) + "</p>" +
            "<p>CIF: " + esc(c.getOrDefault("taxId", "").toString()) + "</p></div>";
    }

    private String renderCustomerInfo(Map<String, Object> ctx, String alignClass) {
        @SuppressWarnings("unchecked")
        var c = (Map<String, Object>) ctx.getOrDefault("customer", Map.of());
        return "<div class=\"" + alignClass + "\"><h3>Client</h3>" +
            "<p>" + esc(c.getOrDefault("name", "").toString()) + "</p>" +
            "<p>" + esc(c.getOrDefault("address", "").toString()) + "</p>" +
            "<p>CIF: " + esc(c.getOrDefault("taxId", "").toString()) + "</p>" +
            "<p>Tel: " + esc(c.getOrDefault("phone", "").toString()) + " | Email: " + esc(c.getOrDefault("email", "").toString()) + "</p></div>";
    }

    private String renderTable(Map<String, Object> ctx) {
        @SuppressWarnings("unchecked")
        var calculated = (Map<String, Object>) ctx.getOrDefault("calculated", Map.of());
        @SuppressWarnings("unchecked")
        var lines = (List<Map<String, Object>>) calculated.getOrDefault("lines", List.of());

        StringBuilder sb = new StringBuilder("<table><thead><tr>");
        sb.append("<th>Descripció</th><th class=\"text-right\">Quantitat</th>");
        sb.append("<th class=\"text-right\">Preu/ud</th><th class=\"text-right\">Total</th>");
        sb.append("</tr></thead><tbody>");
        for (var line : lines) {
            sb.append("<tr>");
            sb.append("<td>").append(esc(line.getOrDefault("description", "").toString())).append("</td>");
            sb.append("<td class=\"text-right\">").append(line.get("quantity")).append("</td>");
            sb.append("<td class=\"text-right\">").append(formatPrice((Number) line.get("unitPrice"))).append("</td>");
            sb.append("<td class=\"text-right\">").append(formatPrice((Number) line.get("total"))).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String renderLogo(Map<String, Object> ctx, com.fasterxml.jackson.databind.JsonNode config, String alignClass) {
        String src = config.path("src").asText("");
        if (src.isBlank()) {
            @SuppressWarnings("unchecked")
            var company = (Map<String, Object>) ctx.getOrDefault("company", Map.of());
            src = company.getOrDefault("logo", "").toString();
        }
        if (src.isBlank()) {
            return "<p class=\"" + alignClass + "\" style=\"font-size: 10px; color: #aaa; border: 1px dashed #ddd; padding: 8px; display: inline-block; min-width: 100px; text-align: center;\">[Logo]</p>";
        }
        return "<div class=\"" + alignClass + "\"><img src=\"" + esc(src) + "\" style=\"max-height: 80px; max-width: 200px;\" alt=\"Logo\" /></div>";
    }

    private String renderImage(com.fasterxml.jackson.databind.JsonNode config, String alignClass) {
        String src = config.path("src").asText("");
        if (src.isBlank()) return "";
        String alt = esc(config.path("alt").asText(""));
        return "<div class=\"" + alignClass + "\"><img src=\"" + esc(src) + "\" style=\"max-width: 100%;\" alt=\"" + alt + "\" /></div>";
    }

    private String renderPlaceholderBlock(String alignClass, String label) {
        return "<p class=\"" + alignClass + "\" style=\"font-size: 10px; color: #888; border: 1px dashed #ccc; padding: 4px 8px; display: inline-block;\">[" + label + "]</p>";
    }

    @SuppressWarnings("unchecked")
    private String resolveCtx(Map<String, Object> ctx, String source, String field) {
        var map = (Map<String, Object>) ctx.getOrDefault(source, Map.of());
        Object val = map.get(field);
        return val != null ? esc(val.toString()) : "{{" + source + "." + field + "}}";
    }

    private String resolvePlaceholder(com.fasterxml.jackson.databind.JsonNode config, String key, String fallback) {
        String text = config.path(key).asText(fallback);
        Pattern p = Pattern.compile("\\{\\{(\\w+)\\.(\\w+)\\}}");
        Matcher m = p.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "{{" + m.group(1) + "." + m.group(2) + "}}");
        }
        m.appendTail(sb);
        return esc(sb.toString());
    }

    private String resolveHtml(Map<String, Object> ctx, String html) {
        if (html == null || html.isBlank()) return "";
        Pattern p = Pattern.compile("\\{\\{(\\w+)\\.(\\w+)\\}}");
        Matcher m = p.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = resolveCtx(ctx, m.group(1), m.group(2));
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String formatPrice(Number n) {
        return NumberFormat.getCurrencyInstance(Locale.of("es", "ES")).format(n);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void saveVersionSnapshot(DocumentTemplate t, String notes) {
        var v = new DocumentTemplateVersion();
        v.setTemplateId(t.getId());
        v.setVersion(t.getVersion());
        v.setLayout(t.getLayout());
        v.setDataBindings(t.getDataBindings());
        v.setStyles(t.getStyles());
        v.setNotes(notes);
        versionRepo.save(v);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj != null ? obj : Map.of());
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private TemplateResponse toTemplateResponse(DocumentTemplate t) {
        return new TemplateResponse(t.getId(), t.getTenantId(), t.getName(), t.getDocumentType(),
            t.getVersion(), t.getActive(), t.getLayout(), t.getDataBindings(), t.getStyles(),
            t.getCreatedAt(), t.getUpdatedAt());
    }

    private DocumentResponse toDocumentResponse(GeneratedDocument d) {
        return new DocumentResponse(d.getId(), d.getTenantId(), d.getTemplateId(),
            d.getTemplateVersion(), d.getNumber(), d.getStatus(),
            d.getHtmlContent(), d.getPdfUrl(), d.getGeneratedAt(), d.getCreatedAt());
    }

    public String getDocumentPdfUrl(UUID documentId) {
        var d = documentRepo.findById(documentId)
            .orElseThrow(() -> new NoSuchElementException("Document not found: " + documentId));
        if (d.getStorageProvider() != null && d.getStorageFileId() != null) {
            try {
                var provider = storageRouter.getProvider(d.getTenantId());
                var signedUrl = provider.getSignedUrl(d.getStorageFileId(), Duration.ofDays(1));
                if (signedUrl != null) return signedUrl;
            } catch (Exception e) {
                log.warn("Failed to refresh signed URL: {}", e.getMessage());
            }
        }
        return d.getPdfUrl() != null ? d.getPdfUrl() : "/api/v1/documents/" + documentId + "/pdf";
    }

    // ── Opció A: Exporta plantilla a Google Docs ──────────────────

    public record ExportDriveResult(String fileId, String webViewLink, String title) {}

    public ExportDriveResult exportTemplateToDrive(UUID templateId, UUID tenantId) {
        var t = templateRepo.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + templateId));
        String html = previewTemplate(templateId);
        String title = t.getName() + " — plantilla";
        var result = googleOrchestrator.exportHtmlToGoogleDocs(tenantId, html, title);
        return new ExportDriveResult(result.fileId(), result.webViewLink(), title);
    }

    // ── Opció B: Importa plantilla des d'un PDF extern ────────────

    private static final String BLOCK_TYPES_HINT = """
        Tipus de blocs disponibles:
        document_title, document_number, document_date, document_validity,
        company_info, customer_info, products_table, services_table,
        subtotal, discount, tax, total,
        text (amb camp "text"), rich_text (amb camp "html"),
        separator, terms (amb camp "text"), conditions (amb camp "text"),
        signature, logo, page_break
        """;

    @Transactional
    public TemplateResponse importFromPdf(UUID tenantId, byte[] pdfBytes, String documentType, String templateName) {
        String extractedText = extractPdfText(pdfBytes);

        String prompt = """
                Analitza aquest document (extret d'un PDF) i genera un array JSON de blocs per al sistema de plantilles AMG.

                Tipus de document: %s

                %s

                Regles:
                - Cada bloc és un objecte JSON amb el camp "type" obligatori
                - Per als blocs "text", "terms", "conditions": afegeix el camp "text" amb el contingut real
                - Per als blocs "rich_text": afegeix el camp "html" amb contingut HTML simple
                - No inventes camps que no existeixin a la llista de blocs
                - Retorna ÚNICAMENT l'array JSON, sense cap text addicional

                Document:
                ---
                %s
                ---
                """.formatted(documentType, BLOCK_TYPES_HINT, extractedText.substring(0, Math.min(extractedText.length(), 3000)));

        var ai = aiProviderRouter.forModel("claude-haiku-4-5-20251001");
        String layoutJson = ai.chat(
                "Ets un analitzador de documents que genera JSON estructurat per a plantilles. Retorna sempre JSON vàlid.",
                List.of(),
                prompt
        );

        // Neteja la resposta: extreu l'array JSON
        if (layoutJson != null) {
            int start = layoutJson.indexOf('[');
            int end = layoutJson.lastIndexOf(']');
            if (start >= 0 && end > start) {
                layoutJson = layoutJson.substring(start, end + 1);
            }
        }

        // Valida que és JSON vàlid
        try {
            objectMapper.readTree(layoutJson);
        } catch (Exception e) {
            log.warn("Claude va generar JSON invàlid, usant layout mínim: {}", e.getMessage());
            layoutJson = """
                    [
                      {"type":"company_info"},
                      {"type":"separator"},
                      {"type":"customer_info"},
                      {"type":"document_number"},
                      {"type":"document_date"},
                      {"type":"services_table"},
                      {"type":"subtotal"},
                      {"type":"tax"},
                      {"type":"total"},
                      {"type":"signature"}
                    ]
                    """;
        }

        var t = new DocumentTemplate();
        t.setTenantId(tenantId);
        t.setName(templateName != null && !templateName.isBlank() ? templateName : "Plantilla importada");
        t.setDocumentType(documentType != null ? documentType : "quote");
        t.setLayout(layoutJson);
        t.setDataBindings("{}");
        t.setStyles("{}");
        var saved = templateRepo.save(t);
        saveVersionSnapshot(saved, "Importat des de PDF");
        return toTemplateResponse(saved);
    }

    private String extractPdfText(byte[] pdfBytes) {
        try (var doc = PDDocument.load(pdfBytes)) {
            var stripper = new PDFTextStripper();
            return stripper.getText(doc);
        } catch (Exception e) {
            throw new IllegalArgumentException("No s'ha pogut llegir el PDF: " + e.getMessage(), e);
        }
    }
}
