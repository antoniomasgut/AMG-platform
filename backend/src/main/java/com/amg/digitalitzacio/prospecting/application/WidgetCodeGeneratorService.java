package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.domain.Prospect;
import org.springframework.stereotype.Service;

/**
 * Genera snippets JS de chat widget + WhatsApp per a negocis amb web existent,
 * adaptats al CMS/framework detectat.
 */
@Service
public class WidgetCodeGeneratorService {

    private static final String CDN = "https://cdn.amgdl.com";
    private static final String DEMO_TENANT = "DEMO_PROSPECT";

    public String generate(Prospect prospect) {
        if (!Boolean.TRUE.equals(prospect.getHasWebsite())) return null;

        var cms = prospect.getCmsDetected();
        var phone = prospect.getPhone() != null ? prospect.getPhone().replaceAll("\\D", "") : "";
        var waPhone = phone.startsWith("34") ? phone : "34" + phone;

        var chatSnippet = buildChatSnippet();
        var waSnippet = buildWaSnippet(waPhone, prospect.getName());
        var instructions = buildInstructions(cms);

        return String.format("""
            <!-- ═══════════════════════════════════════════ -->
            <!-- Widget de Chat IA — AMG Digitalitzacions   -->
            <!-- Negoci: %s | CMS: %s -->
            <!-- ═══════════════════════════════════════════ -->

            %s

            <!-- ═══════════════════════════════════════════ -->
            <!-- Botó WhatsApp flotant                      -->
            <!-- ═══════════════════════════════════════════ -->

            %s

            <!-- ═══════════════════════════════════════════ -->
            <!-- Instruccions d'instal·lació: %s -->
            <!-- %s -->
            <!-- ═══════════════════════════════════════════ -->
            """,
            prospect.getName(),
            cms != null ? cms : "HTML",
            chatSnippet,
            waSnippet,
            cms != null ? cms : "HTML",
            instructions);
    }

    private String buildChatSnippet() {
        return "<script>\n" +
            "  window.AMGChat = {\n" +
            "    tenantId: '" + DEMO_TENANT + "',\n" +
            "    position: 'bottom-right',\n" +
            "    theme: 'light',\n" +
            "    primaryColor: '#FF6B00'\n" +
            "  };\n" +
            "</script>\n" +
            "<script src=\"" + CDN + "/widget.js\" async defer></script>";
    }

    private String buildWaSnippet(String waPhone, String businessName) {
        var msg = java.net.URLEncoder.encode(
            "Hola, m'agradaria obtenir més informació sobre " + businessName,
            java.nio.charset.StandardCharsets.UTF_8);
        return String.format("""
            <a href="https://wa.me/%s?text=%s"
               target="_blank" rel="noopener noreferrer"
               style="position:fixed;bottom:24px;right:24px;z-index:9998;display:block;
                      width:56px;height:56px;border-radius:50%;
                      background:#25D366;box-shadow:0 4px 12px rgba(0,0,0,.25)">
              <img src="%s/wa-btn.svg" width="56" height="56" alt="WhatsApp">
            </a>""", waPhone, msg, CDN);
    }

    private String buildInstructions(String cms) {
        if (cms == null) return "Afegeix el codi just abans del tancament </body>";
        return switch (cms) {
            case "WordPress"  -> "WordPress: Plugin 'Header Footer Code Manager' o functions.php";
            case "Shopify"    -> "Shopify: Online Store > Themes > Edit code > theme.liquid > abans </body>";
            case "Wix"        -> "Wix: Settings > Custom Code > Body (end) > Add to all pages";
            case "PrestaShop" -> "PrestaShop: Design > Theme & Logo > Advanced customization > custom.js";
            case "Squarespace"-> "Squarespace: Settings > Advanced > Code Injection > Footer";
            case "Webflow"    -> "Webflow: Project settings > Custom code > Footer code";
            case "Next.js"    -> "Next.js: src/app/layout.tsx o pages/_document.tsx > <body>";
            case "React"      -> "React: public/index.html > abans </body>";
            case "Vue"        -> "Vue: public/index.html > abans </body>";
            default -> "Afegeix el codi just abans del tancament </body>";
        };
    }
}
