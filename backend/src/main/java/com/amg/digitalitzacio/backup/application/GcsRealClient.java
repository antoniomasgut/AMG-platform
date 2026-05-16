package com.amg.digitalitzacio.backup.application;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.backup.provider", havingValue = "gcs")
@Slf4j
public class GcsRealClient implements GcsClient {

    @Value("${app.backup.gcs.project-id:#{null}}")
    private String projectId;

    private Storage storage;

    @PostConstruct
    void init() {
        try {
            var builder = StorageOptions.newBuilder();
            if (projectId != null && !projectId.isBlank()) {
                builder.setProjectId(projectId);
            }
            storage = builder.build().getService();
            log.info("GcsRealClient initialized (project: {})", projectId != null ? projectId : "default");
        } catch (Exception e) {
            log.error("Failed to initialize GCS client: {}", e.getMessage());
        }
    }

    @Override
    public String upload(String bucket, String path, byte[] data, String contentType) {
        try {
            var blobId = BlobId.of(bucket, path);
            var blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType != null ? contentType : "application/octet-stream")
                    .build();
            storage.create(blobInfo, data);
            log.info("Uploaded {} bytes to gs://{}/{}", data.length, bucket, path);
            return "gs://" + bucket + "/" + path;
        } catch (Exception e) {
            log.error("Failed to upload to gs://{}/{}: {}", bucket, path, e.getMessage());
            throw new RuntimeException("GCS upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String bucket, String path) {
        try {
            var blobId = BlobId.of(bucket, path);
            var blob = storage.get(blobId);
            if (blob == null || !blob.exists()) {
                log.warn("File not found: gs://{}/{}", bucket, path);
                return new byte[0];
            }
            return blob.getContent();
        } catch (Exception e) {
            log.error("Failed to download gs://{}/{}: {}", bucket, path, e.getMessage());
            throw new RuntimeException("GCS download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String bucket, String path) {
        try {
            var blobId = BlobId.of(bucket, path);
            var result = storage.delete(blobId);
            if (result) {
                log.info("Deleted gs://{}/{}", bucket, path);
            } else {
                log.warn("File not found for deletion: gs://{}/{}", bucket, path);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to delete gs://{}/{}: {}", bucket, path, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(String bucket, String path) {
        try {
            var blobId = BlobId.of(bucket, path);
            var blob = storage.get(blobId);
            return blob != null && blob.exists();
        } catch (Exception e) {
            log.error("Failed to check existence of gs://{}/{}: {}", bucket, path, e.getMessage());
            return false;
        }
    }

    @Override
    public long size(String bucket, String path) {
        try {
            var blobId = BlobId.of(bucket, path);
            var blob = storage.get(blobId);
            if (blob != null && blob.exists()) {
                return blob.getSize();
            }
            return 0;
        } catch (Exception e) {
            log.error("Failed to get size of gs://{}/{}: {}", bucket, path, e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean isConnected() {
        try {
            storage.list(Storage.BucketListOption.pageSize(1));
            return true;
        } catch (Exception e) {
            log.warn("GCS connection check failed: {}", e.getMessage());
            return false;
        }
    }
}
