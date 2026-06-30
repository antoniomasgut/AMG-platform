package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.domain.Prospect;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Analitza la presència digital d'un prospect: SSL, responsive, tech stack,
 * xarxes socials, chat widgets, sistema de reserves, formularis, etc.
 */
@Service
@Slf4j
public class WebAnalyzerService {

    private static final int TIMEOUT_SECONDS = 10;
    private static final int SLOW_WEB_MS = 3000;

    private static final Pattern ANALYTICS_P = Pattern.compile("gtag\\.js|analytics\\.js|googletagmanager\\.com/gtag|google-analytics\\.com", Pattern.CASE_INSENSITIVE);
    private static final Pattern GTM_P       = Pattern.compile("googletagmanager\\.com/gtm\\.js|GTM-[A-Z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PIXEL_P     = Pattern.compile("fbq\\s*\\(\\s*['\"]init['\"]|connect\\.facebook\\.net.*fbevents", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHAT_P      = Pattern.compile("intercom\\.io|crisp\\.chat|tidio|livechat|drift\\.com|tawk\\.to|zendesk.*chat|freshchat|hotjar\\.com", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOOKING_P   = Pattern.compile("calendly\\.com|simplybook|acuity|booksy|reservas|booking-widget|tresbookings|appointy|setmore|square\\.site", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORM_P      = Pattern.compile("<form[^>]*>.*?</form>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FAQ_P       = Pattern.compile("faq|preguntes\\s+freq|preguntas\\s+frec|frequently\\s+asked", Pattern.CASE_INSENSITIVE);
    private static final Pattern CTA_P       = Pattern.compile("reserva|contacta|truca|llama|whatsapp|cita|pressupost|cotiza|book\\s+now|get\\s+started|call\\s+us", Pattern.CASE_INSENSITIVE);
    private static final Pattern VIEWPORT_P  = Pattern.compile("<meta[^>]+name=['\"]viewport['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern WA_P        = Pattern.compile("wa\\.me/|api\\.whatsapp\\.com|whatsapp.*(?:widget|button|chat)", Pattern.CASE_INSENSITIVE);

    // CMS / Frameworks
    private static final Pattern WP_P        = Pattern.compile("/wp-content/|/wp-includes/|wordpress", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHOPIFY_P   = Pattern.compile("cdn\\.shopify\\.com|myshopify\\.com|Shopify\\.theme", Pattern.CASE_INSENSITIVE);
    private static final Pattern WIX_P       = Pattern.compile("wix\\.com|wixstatic\\.com|wixsite\\.com", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRESTASHOP_P= Pattern.compile("prestashop|presta-shop", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQUARESPACE_P=Pattern.compile("squarespace\\.com|static\\.squarespace", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEBFLOW_P   = Pattern.compile("webflow\\.com|webflow\\.io", Pattern.CASE_INSENSITIVE);
    private static final Pattern NEXT_P      = Pattern.compile("__NEXT_DATA__|/_next/static/", Pattern.CASE_INSENSITIVE);
    private static final Pattern REACT_P     = Pattern.compile("react\\.development\\.js|react\\.production\\.min|__reactFiber", Pattern.CASE_INSENSITIVE);
    private static final Pattern VUE_P       = Pattern.compile("vue\\.min\\.js|__vue__|data-v-[a-f0-9]+", Pattern.CASE_INSENSITIVE);

    // Social networks
    private static final Pattern FB_P        = Pattern.compile("facebook\\.com/(?!sharer|dialog|tr\\?)[\\w.]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern LI_P        = Pattern.compile("linkedin\\.com/(?:company|in)/[\\w-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TT_P        = Pattern.compile("tiktok\\.com/@[\\w.]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern YT_P        = Pattern.compile("youtube\\.com/(?:channel|c|user|@)[\\w-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern IG_P        = Pattern.compile("instagram\\.com/[\\w.]+", Pattern.CASE_INSENSITIVE);

    public void analyze(Prospect prospect) {
        if (prospect.getWebsite() == null || prospect.getWebsite().isBlank()) return;

        var url = prospect.getWebsite();
        if (!url.startsWith("http")) url = "https://" + url;

        try {
            var client = buildClient();
            var t0 = System.currentTimeMillis();

            String html = null;
            boolean ssl = url.startsWith("https://");
            try {
                html = client.get()
                    .uri(url)
                    .header("User-Agent", "AMGBot/1.0 (+https://amgdl.com)")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();
            } catch (Exception e) {
                // Prova sense SSL si falla
                if (ssl) {
                    try {
                        html = client.get()
                            .uri(url.replace("https://", "http://"))
                            .header("User-Agent", "AMGBot/1.0 (+https://amgdl.com)")
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                            .block();
                        ssl = false;
                    } catch (Exception e2) {
                        log.debug("WebAnalyzer: cannot fetch {}: {}", url, e2.getMessage());
                    }
                }
            }

            var loadMs = (int)(System.currentTimeMillis() - t0);

            prospect.setHasSsl(ssl);
            prospect.setWebLoadMs(loadMs);

            if (html != null) {
                prospect.setIsResponsive(VIEWPORT_P.matcher(html).find());
                prospect.setHasAnalytics(ANALYTICS_P.matcher(html).find());
                prospect.setHasGtm(GTM_P.matcher(html).find());
                prospect.setHasPixel(PIXEL_P.matcher(html).find());
                prospect.setHasChatWidget(CHAT_P.matcher(html).find());
                prospect.setHasBookingSystem(BOOKING_P.matcher(html).find());
                prospect.setHasContactForm(FORM_P.matcher(html).find());
                prospect.setHasFaq(FAQ_P.matcher(html).find());
                prospect.setHasClearCta(CTA_P.matcher(html).find());

                // WhatsApp widget (complement al hasWhatsapp de Places)
                if (!Boolean.TRUE.equals(prospect.getHasWhatsapp())) {
                    prospect.setHasWhatsapp(WA_P.matcher(html).find());
                }

                // CMS / Framework
                prospect.setCmsDetected(detectCms(html));
                prospect.setTechStack(buildTechStack(html));

                // Social networks
                extractSocialLinks(html, prospect);
            }

            prospect.setWebAnalyzedAt(Instant.now());
            log.debug("WebAnalyzer: {} analyzed in {}ms (ssl={}, cms={})", url, loadMs, ssl, prospect.getCmsDetected());

        } catch (Exception e) {
            log.debug("WebAnalyzer error for {}: {}", url, e.getMessage());
        }
    }

    private String detectCms(String html) {
        if (WP_P.matcher(html).find()) return "WordPress";
        if (SHOPIFY_P.matcher(html).find()) return "Shopify";
        if (WIX_P.matcher(html).find()) return "Wix";
        if (PRESTASHOP_P.matcher(html).find()) return "PrestaShop";
        if (SQUARESPACE_P.matcher(html).find()) return "Squarespace";
        if (WEBFLOW_P.matcher(html).find()) return "Webflow";
        if (NEXT_P.matcher(html).find()) return "Next.js";
        if (REACT_P.matcher(html).find()) return "React";
        if (VUE_P.matcher(html).find()) return "Vue";
        return null;
    }

    private String buildTechStack(String html) {
        var techs = new ArrayList<String>();
        if (ANALYTICS_P.matcher(html).find()) techs.add("Google Analytics");
        if (GTM_P.matcher(html).find()) techs.add("Google Tag Manager");
        if (PIXEL_P.matcher(html).find()) techs.add("Meta Pixel");
        if (CHAT_P.matcher(html).find()) techs.add("Chat widget");
        if (BOOKING_P.matcher(html).find()) techs.add("Booking system");
        var cms = detectCms(html);
        if (cms != null) techs.add(cms);
        return techs.isEmpty() ? null : String.join(", ", techs);
    }

    private void extractSocialLinks(String html, Prospect prospect) {
        var fbM = FB_P.matcher(html);
        if (fbM.find()) {
            prospect.setHasFacebook(true);
            prospect.setFacebookUrl("https://www." + fbM.group());
        }
        var liM = LI_P.matcher(html);
        if (liM.find()) {
            prospect.setHasLinkedin(true);
            prospect.setLinkedinUrl("https://www." + liM.group());
        }
        var ttM = TT_P.matcher(html);
        if (ttM.find()) {
            prospect.setHasTiktok(true);
            prospect.setTiktokUrl("https://www." + ttM.group());
        }
        var ytM = YT_P.matcher(html);
        if (ytM.find()) {
            prospect.setYoutubeUrl("https://www." + ytM.group());
        }
        if (!Boolean.TRUE.equals(prospect.getHasInstagram())) {
            var igM = IG_P.matcher(html);
            if (igM.find()) {
                prospect.setHasInstagram(true);
                prospect.setInstagram(igM.group().replace("instagram.com/", ""));
            }
        }
    }

    private WebClient buildClient() {
        try {
            var sslCtx = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
            var httpClient = HttpClient.create()
                .secure(s -> s.sslContext(sslCtx))
                .followRedirect(true)
                .responseTimeout(Duration.ofSeconds(TIMEOUT_SECONDS));
            return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        } catch (Exception e) {
            return WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        }
    }
}
