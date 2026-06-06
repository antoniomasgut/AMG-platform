package com.amg.digitalitzacio.shared.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.storage", name = "default-provider", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalStorageProvider implements StorageProvider {

    private final Path basePath;

    public LocalStorageProvider(@Value("${app.storage.path:/data/assets}") String path) {
        this.basePath = Path.of(path);
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(basePath);
            log.info("LocalStorageProvider initialized at {}", basePath);
        } catch (Exception e) {
            log.warn("Could not create storage directory: {}", e.getMessage());
        }
    }

    @Override
    public StorageFile upload(InputStream data, String fileName, String mimeType) {
        try {
            var id = UUID.randomUUID().toString();
            var ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
            var target = basePath.resolve(id + ext);
            try (OutputStream os = Files.newOutputStream(target)) {
                data.transferTo(os);
            }
            var size = Files.size(target);
            log.info("Uploaded {} -> {} ({} bytes)", fileName, target, size);
            return new StorageFile(id, fileName, mimeType, size, "local");
        } catch (Exception e) {
            throw new RuntimeException("Local upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String fileId) {
        try {
            var match = Files.list(basePath)
                .filter(f -> f.getFileName().toString().startsWith(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
            return Files.newInputStream(match);
        } catch (Exception e) {
            throw new RuntimeException("Local download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileId) {
        try {
            Files.list(basePath)
                .filter(f -> f.getFileName().toString().startsWith(fileId))
                .forEach(f -> { try { Files.delete(f); } catch (Exception ignored) {} });
        } catch (Exception e) {
            log.warn("Local delete failed for {}: {}", fileId, e.getMessage());
        }
    }

    @Override
    public boolean exists(String fileId) {
        try {
            return Files.list(basePath)
                .anyMatch(f -> f.getFileName().toString().startsWith(fileId));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getSignedUrl(String fileId, Duration expiry) {
        return "/api/v1/storage/files/" + fileId;
    }

    @Override
    public FileMetadata getMetadata(String fileId) {
        try {
            var match = Files.list(basePath)
                .filter(f -> f.getFileName().toString().startsWith(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
            var attrs = Files.readAttributes(match, java.nio.file.attribute.BasicFileAttributes.class);
            var name = match.getFileName().toString();
            var ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
            return new FileMetadata(fileId, name, "application/" + ext, attrs.size(), attrs.creationTime().toInstant(), null);
        } catch (Exception e) {
            throw new RuntimeException("Metadata lookup failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() { return "local"; }
}
