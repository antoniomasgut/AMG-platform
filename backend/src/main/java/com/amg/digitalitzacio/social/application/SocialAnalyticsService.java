package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.engine.domain.LandingVisitDailyRepository;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.LeadSource;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sincronitza mètriques d'engagement dels posts publicats (Mòdul 55, feature 2)
 * i construeix el resum setmanal per al tenant. Best-effort: si el Graph API
 * no retorna una mètrica, es deixa a null sense fallar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAnalyticsService {

    private static final String GRAPH_URL = "https://graph.facebook.com";
    private static final String API_VERSION = "v22.0";

    private final SocialPostRepository postRepository;
    private final SocialMetaConfigRepository metaConfigRepo;
    private final VaultEncryption vaultEncryption;
    private final ObjectMapper objectMapper;
    private final LandingVisitDailyRepository visitDailyRepository;
    private final LeadRepository leadRepository;

    /** Sincronitza les mètriques dels posts publicats del tenant en els últims 30 dies */
    public void syncMetrics(UUID tenantId) {
        var meta = metaConfigRepo.findByTenantId(tenantId).orElse(null);
        if (meta == null || meta.getPageAccessTokenEncrypted() == null) return;

        String token;
        try {
            token = vaultEncryption.decrypt(meta.getPageAccessTokenEncrypted());
        } catch (Exception e) {
            log.warn("Social analytics: no s'ha pogut desxifrar el token del tenant {}", tenantId);
            return;
        }

        var since = Instant.now().minus(Duration.ofDays(30));
        var posts = postRepository.findPublishedSince(tenantId, since);
        var client = WebClient.builder().baseUrl(GRAPH_URL).build();

        for (var post : posts) {
            if (post.getExternalPostId() == null) continue;
            try {
                if ("INSTAGRAM".equals(post.getNetwork())) {
                    fetchInstagramMetrics(client, post, token);
                } else if ("FACEBOOK".equals(post.getNetwork())) {
                    fetchFacebookMetrics(client, post, token);
                } else {
                    continue; // Google Business no exposa aquestes mètriques per post
                }
                post.setMetricsSyncedAt(Instant.now());
                postRepository.save(post);
            } catch (Exception e) {
                log.debug("No s'han pogut obtenir mètriques del post {}: {}", post.getId(), e.getMessage());
            }
        }
    }

    private void fetchInstagramMetrics(WebClient client, SocialPost post, String token) {
        // like_count + comments_count via camps del media
        var fields = get(client, "/" + API_VERSION + "/" + post.getExternalPostId()
                + "?fields=like_count,comments_count&access_token=" + token);
        if (fields != null) {
            if (fields.has("like_count")) post.setLikes(fields.path("like_count").asInt());
            if (fields.has("comments_count")) post.setComments(fields.path("comments_count").asInt());
        }
        // reach via insights
        var insights = get(client, "/" + API_VERSION + "/" + post.getExternalPostId()
                + "/insights?metric=reach&access_token=" + token);
        Integer reach = readInsightValue(insights, "reach");
        if (reach != null) post.setReach(reach);
    }

    private void fetchFacebookMetrics(WebClient client, SocialPost post, String token) {
        // likes + comments summary
        var fields = get(client, "/" + API_VERSION + "/" + post.getExternalPostId()
                + "?fields=likes.summary(true),comments.summary(true)&access_token=" + token);
        if (fields != null) {
            var likes = fields.path("likes").path("summary").path("total_count");
            if (!likes.isMissingNode()) post.setLikes(likes.asInt());
            var comments = fields.path("comments").path("summary").path("total_count");
            if (!comments.isMissingNode()) post.setComments(comments.asInt());
        }
        // reach (impressions úniques)
        var insights = get(client, "/" + API_VERSION + "/" + post.getExternalPostId()
                + "/insights?metric=post_impressions_unique&access_token=" + token);
        Integer reach = readInsightValue(insights, "post_impressions_unique");
        if (reach != null) post.setReach(reach);
    }

    private Integer readInsightValue(JsonNode insights, String metric) {
        if (insights == null) return null;
        var data = insights.path("data");
        if (!data.isArray()) return null;
        for (JsonNode m : data) {
            if (metric.equals(m.path("name").asText())) {
                var values = m.path("values");
                if (values.isArray() && values.size() > 0) {
                    return values.get(0).path("value").asInt();
                }
            }
        }
        return null;
    }

    private JsonNode get(WebClient client, String uri) {
        try {
            var body = client.get().uri(uri)
                    .retrieve().bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(12)).block();
            return body == null ? null : objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    /** Construeix el text del resum setmanal (últims 7 dies). Retorna null si no hi ha posts. */
    public String buildWeeklyDigest(UUID tenantId) {
        var since = Instant.now().minus(Duration.ofDays(7));
        var posts = postRepository.findPublishedSince(tenantId, since);
        if (posts.isEmpty()) return null;

        int totalReach = posts.stream().mapToInt(p -> p.getReach() != null ? p.getReach() : 0).sum();
        int totalLikes = posts.stream().mapToInt(p -> p.getLikes() != null ? p.getLikes() : 0).sum();
        int totalComments = posts.stream().mapToInt(p -> p.getComments() != null ? p.getComments() : 0).sum();

        var top = posts.stream()
                .max((a, b) -> Integer.compare(
                        (a.getLikes() != null ? a.getLikes() : 0) + (a.getReach() != null ? a.getReach() : 0),
                        (b.getLikes() != null ? b.getLikes() : 0) + (b.getReach() != null ? b.getReach() : 0)))
                .orElse(null);

        var sb = new StringBuilder();
        sb.append("📊 <b>Resum setmanal de xarxes</b>\n\n");
        sb.append("📝 Posts publicats: <b>").append(posts.size()).append("</b>\n");
        sb.append("👁 Abast total: <b>").append(totalReach).append("</b>\n");
        sb.append("❤️ Likes: <b>").append(totalLikes).append("</b>\n");
        sb.append("💬 Comentaris: <b>").append(totalComments).append("</b>\n");
        if (top != null && top.getCaption() != null) {
            String c = top.getCaption().length() > 80 ? top.getCaption().substring(0, 77) + "…" : top.getCaption();
            sb.append("\n🏆 Post destacat: \"").append(escapeHtml(c)).append("\"");
        }
        appendTrafficSection(sb, tenantId, since);
        return sb.toString();
    }

    /**
     * Trànsit i leads atribuïts a xarxes (P2): visites de landing per utm_source
     * i leads captats des de cada xarxa en els últims 7 dies. Best-effort.
     */
    private void appendTrafficSection(StringBuilder sb, UUID tenantId, Instant since) {
        try {
            var sinceDate = java.time.LocalDate.ofInstant(since, java.time.ZoneId.of("Europe/Madrid"));
            var bySource = visitDailyRepository.sumByTenantSince(tenantId, sinceDate);
            var socialVisits = bySource.stream()
                    .filter(v -> !"direct".equals(v.getSource()) && !"other".equals(v.getSource()))
                    .toList();

            long igLeads = leadRepository.countByTenantIdAndSourceSince(tenantId, LeadSource.INSTAGRAM, since);
            long fbLeads = leadRepository.countByTenantIdAndSourceSince(tenantId, LeadSource.FACEBOOK, since);

            if (socialVisits.isEmpty() && igLeads == 0 && fbLeads == 0) return;

            sb.append("\n\n🔗 <b>Trànsit des de xarxes → web</b>\n");
            var labels = java.util.Map.of(
                "instagram", "Instagram", "facebook", "Facebook",
                "google_business", "Google Business", "linkedin", "LinkedIn",
                "whatsapp", "WhatsApp", "email", "Email");
            for (var v : socialVisits) {
                sb.append("• ").append(labels.getOrDefault(v.getSource(), v.getSource()))
                  .append(": <b>").append(v.getViews()).append("</b> visites\n");
            }
            long totalLeads = igLeads + fbLeads;
            if (totalLeads > 0) {
                sb.append("🎯 Contactes nous des de xarxes: <b>").append(totalLeads).append("</b>");
                if (igLeads > 0 && fbLeads > 0) {
                    sb.append(" (IG ").append(igLeads).append(" · FB ").append(fbLeads).append(")");
                }
            }
        } catch (Exception e) {
            log.debug("No s'ha pogut afegir la secció de trànsit al digest de {}: {}", tenantId, e.getMessage());
        }
    }

    private String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public List<SocialPost> publishedLastDays(UUID tenantId, int days) {
        return postRepository.findPublishedSince(tenantId, Instant.now().minus(Duration.ofDays(days)));
    }
}
