package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.shared.storage.FileMetadata;
import com.amg.digitalitzacio.shared.storage.StorageFile;
import com.amg.digitalitzacio.shared.storage.StorageProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Slf4j
public class GoogleDriveStorageProvider implements StorageProvider {

    private final String accessToken;
    private final String rootFolderId;
    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public GoogleDriveStorageProvider(String accessToken, String rootFolderId) {
        this.accessToken = accessToken;
        this.rootFolderId = rootFolderId;
    }

    @Override
    public StorageFile upload(InputStream data, String fileName, String mimeType) {
        try {
            var bytes = data.readAllBytes();
            var boundary = "boundary_" + System.currentTimeMillis();

            var metadata = new java.util.LinkedHashMap<String, Object>();
            metadata.put("name", fileName);
            if (rootFolderId != null) {
                metadata.put("parents", java.util.List.of(rootFolderId));
            }

            var bodyBuilder = new StringBuilder();
            bodyBuilder.append("--").append(boundary).append("\r\n");
            bodyBuilder.append("Content-Type: application/json; charset=UTF-8\r\n\r\n");
            bodyBuilder.append(gson.toJson(metadata)).append("\r\n");
            bodyBuilder.append("--").append(boundary).append("\r\n");
            bodyBuilder.append("Content-Type: ").append(mimeType).append("\r\n\r\n");
            bodyBuilder.append(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1)).append("\r\n");
            bodyBuilder.append("--").append(boundary).append("--\r\n");

            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/related; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(bodyBuilder.toString()))
                .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Drive upload failed: " + response.statusCode() + " " + response.body());
            }

            var json = gson.fromJson(response.body(), JsonObject.class);
            var fileId = json.get("id").getAsString();
            log.info("Uploaded {} -> Drive fileId={}", fileName, fileId);
            return new StorageFile(fileId, fileName, mimeType, bytes.length, "google_drive");
        } catch (Exception e) {
            throw new RuntimeException("Google Drive upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String fileId) {
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Drive download failed: " + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("Google Drive download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileId) {
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/drive/v3/files/" + fileId))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();
            http.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Deleted Drive fileId={}", fileId);
        } catch (Exception e) {
            log.warn("Google Drive delete failed for {}: {}", fileId, e.getMessage());
        }
    }

    @Override
    public boolean exists(String fileId) {
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/drive/v3/files/" + fileId + "?fields=id"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getSignedUrl(String fileId, Duration expiry) {
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/drive/v3/files/" + fileId + "?fields=webViewLink"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                var json = gson.fromJson(response.body(), JsonObject.class);
                if (json.has("webViewLink")) {
                    return json.get("webViewLink").getAsString();
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get webViewLink for {}: {}", fileId, e.getMessage());
            return null;
        }
    }

    @Override
    public FileMetadata getMetadata(String fileId) {
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/drive/v3/files/" + fileId + "?fields=id,name,mimeType,size,createdTime"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Drive metadata failed: " + response.statusCode());
            }
            var json = gson.fromJson(response.body(), JsonObject.class);
            var size = json.has("size") ? json.get("size").getAsLong() : 0L;
            var created = json.has("createdTime") ? Instant.parse(json.get("createdTime").getAsString()) : Instant.now();
            return new FileMetadata(
                json.get("id").getAsString(),
                json.get("name").getAsString(),
                json.has("mimeType") ? json.get("mimeType").getAsString() : "application/octet-stream",
                size, created, null);
        } catch (Exception e) {
            throw new RuntimeException("Google Drive metadata failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() { return "google_drive"; }
}
