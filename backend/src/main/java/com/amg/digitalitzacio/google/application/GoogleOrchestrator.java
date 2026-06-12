package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.google.api.dto.GoogleStatusResponse;
import com.amg.digitalitzacio.google.api.dto.ModuleConfigRequest;
import com.amg.digitalitzacio.google.api.dto.SendMailRequest;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfig;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfigRepository;
import com.amg.digitalitzacio.shared.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleOrchestrator {

    private final GoogleTokenService tokenService;
    private final GoogleModuleConfigRepository moduleConfigRepo;
    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @Transactional(readOnly = true)
    public GoogleStatusResponse getStatus(UUID tenantId) {
        var conn = tokenService.getValidCredentials(tenantId);
        var config = moduleConfigRepo.findByTenantId(tenantId)
            .orElse(GoogleModuleConfig.defaults(tenantId));
        return new GoogleStatusResponse(true, conn.email(),
            config.isDriveEnabled(), config.isGmailEnabled(),
            config.isCalendarEnabled(), config.isSheetsEnabled());
    }

    @Transactional
    public GoogleModuleConfig updateModules(UUID tenantId, ModuleConfigRequest req) {
        var config = moduleConfigRepo.findByTenantId(tenantId)
            .orElseGet(() -> GoogleModuleConfig.defaults(tenantId));
        config.setDriveEnabled(req.driveEnabled());
        config.setGmailEnabled(req.gmailEnabled());
        config.setCalendarEnabled(req.calendarEnabled());
        config.setSheetsEnabled(req.sheetsEnabled());
        return moduleConfigRepo.save(config);
    }

    public StorageProvider getDriveProvider(UUID tenantId) {
        var creds = tokenService.getValidCredentials(tenantId);
        var config = moduleConfigRepo.findByTenantId(tenantId)
            .orElse(GoogleModuleConfig.defaults(tenantId));
        return new GoogleDriveStorageProvider(creds.accessToken(), config.getDriveFolderId());
    }

    public void sendMail(UUID tenantId, SendMailRequest req) {
        var creds = tokenService.getValidCredentials(tenantId);
        var provider = new GoogleMailProvider(creds.accessToken(), creds.email());
        provider.send(req.to(), req.subject(), req.body(), null, null, null);
    }

    public void sendMailWithAttachment(UUID tenantId, String to, String subject, String body,
                                        String attachmentName, byte[] attachmentData, String attachmentMimeType) {
        var creds = tokenService.getValidCredentials(tenantId);
        var provider = new GoogleMailProvider(creds.accessToken(), creds.email());
        provider.send(to, subject, body, attachmentName, new ByteArrayInputStream(attachmentData), attachmentMimeType);
    }

    public List<DriveFileItem> listDriveFiles(UUID tenantId, String folderId) {
        var creds = tokenService.getValidCredentials(tenantId);
        var query = folderId != null
            ? "'" + folderId + "' in parents and trashed=false"
            : "trashed=false";
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/drive/v3/files?q="
                    + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
                    + "&fields=files(id,name,mimeType,size,createdTime,webViewLink)&orderBy=folder,name"))
                .header("Authorization", "Bearer " + creds.accessToken())
                .GET()
                .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Drive list failed: " + response.statusCode());
            }
            var json = gson.fromJson(response.body(), JsonObject.class);
            var files = json.getAsJsonArray("files");
            var result = new ArrayList<DriveFileItem>();
            if (files != null) {
                for (var el : files) {
                    var obj = el.getAsJsonObject();
                    result.add(new DriveFileItem(
                        obj.get("id").getAsString(),
                        obj.get("name").getAsString(),
                        obj.has("mimeType") ? obj.get("mimeType").getAsString() : "application/octet-stream",
                        obj.has("size") ? obj.get("size").getAsLong() : 0L,
                        obj.has("createdTime") ? Instant.parse(obj.get("createdTime").getAsString()) : Instant.now(),
                        obj.has("webViewLink") ? obj.get("webViewLink").getAsString() : null
                    ));
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Google Drive list failed: " + e.getMessage(), e);
        }
    }

    public List<CalendarEventItem> listCalendarEvents(UUID tenantId) {
        var creds = tokenService.getValidCredentials(tenantId);
        try {
            var now = java.time.Instant.now();
            var weekLater = now.plus(java.time.Duration.ofDays(30));
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/calendar/v3/calendars/primary/events?"
                    + "timeMin=" + now + "&timeMax=" + weekLater
                    + "&orderBy=startTime&singleEvents=true"
                    + "&fields=items(id,summary,description,start,end,htmlLink)"))
                .header("Authorization", "Bearer " + creds.accessToken())
                .GET()
                .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Calendar list failed: " + response.statusCode());
            }
            var json = gson.fromJson(response.body(), JsonObject.class);
            var items = json.getAsJsonArray("items");
            var result = new ArrayList<CalendarEventItem>();
            if (items != null) {
                for (var el : items) {
                    var obj = el.getAsJsonObject();
                    result.add(new CalendarEventItem(
                        obj.get("id").getAsString(),
                        obj.get("summary").getAsString(),
                        obj.has("description") ? obj.get("description").getAsString() : null,
                        obj.getAsJsonObject("start").get("dateTime") != null
                            ? obj.getAsJsonObject("start").get("dateTime").getAsString()
                            : obj.getAsJsonObject("start").get("date").getAsString(),
                        obj.getAsJsonObject("end").get("dateTime") != null
                            ? obj.getAsJsonObject("end").get("dateTime").getAsString()
                            : obj.getAsJsonObject("end").get("date").getAsString(),
                        obj.has("htmlLink") ? obj.get("htmlLink").getAsString() : null
                    ));
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Google Calendar list failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void disconnect(UUID tenantId) {
        tokenService.revoke(tenantId);
        moduleConfigRepo.deleteById(tenantId);
    }

    // ── Export HTML → Google Docs ─────────────────────────────────

    public record DriveDocResult(String fileId, String webViewLink) {}

    /**
     * Puja HTML a Google Drive i el converteix a Google Docs.
     * Retorna l'ID del fitxer i el link de visualització.
     */
    public DriveDocResult exportHtmlToGoogleDocs(UUID tenantId, String html, String title) {
        var creds = tokenService.getValidCredentials(tenantId);
        var config = moduleConfigRepo.findByTenantId(tenantId)
                .orElse(GoogleModuleConfig.defaults(tenantId));

        String boundary = "amg_boundary_" + java.util.UUID.randomUUID().toString().replace("-", "");

        var metadataObj = new JsonObject();
        metadataObj.addProperty("name", title);
        metadataObj.addProperty("mimeType", "application/vnd.google-apps.document");
        if (config.getDriveFolderId() != null && !config.getDriveFolderId().isBlank()) {
            var parents = new JsonArray();
            parents.add(config.getDriveFolderId());
            metadataObj.add("parents", parents);
        }
        String metadata = gson.toJson(metadataObj);

        String body = "--" + boundary + "\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                metadata + "\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n\r\n" +
                html + "\r\n" +
                "--" + boundary + "--";

        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,webViewLink"))
                    .header("Authorization", "Bearer " + creds.accessToken())
                    .header("Content-Type", "multipart/related; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Drive upload failed: " + response.statusCode() + " " + response.body());
            }

            var json = gson.fromJson(response.body(), JsonObject.class);
            return new DriveDocResult(
                    json.get("id").getAsString(),
                    json.has("webViewLink") ? json.get("webViewLink").getAsString() : null
            );
        } catch (Exception e) {
            throw new RuntimeException("Error exportant a Google Docs: " + e.getMessage(), e);
        }
    }
}
