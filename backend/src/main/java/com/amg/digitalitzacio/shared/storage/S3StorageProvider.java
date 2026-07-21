package com.amg.digitalitzacio.shared.storage;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class S3StorageProvider implements StorageProvider {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final String providerName;

    public S3StorageProvider(Map<String, Object> config) {
        var endpoint = (String) config.getOrDefault("endpoint", "");
        var region = (String) config.getOrDefault("region", "eu-central-1");
        var accessKey = (String) config.getOrDefault("accessKey", "");
        var secretKey = (String) config.getOrDefault("secretKey", "");
        this.bucket = (String) config.getOrDefault("bucket", "amg-documents");
        this.providerName = (String) config.getOrDefault("providerName", "minio");

        var builder = S3Client.builder()
            .region(Region.of(region))
            .forcePathStyle(true);

        if (!endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        }

        if (endpoint.contains("minio") || endpoint.contains("localhost") || endpoint.contains(":9000")) {
            builder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build());
        }

        this.s3 = builder.build();
        this.presigner = S3Presigner.builder()
            .region(Region.of(region))
            .endpointOverride(endpoint.isEmpty() ? null : URI.create(endpoint))
            .build();

        ensureBucket();
    }

    private void ensureBucket() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            try {
                s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Created bucket: {}", bucket);
            } catch (Exception ex) {
                log.warn("Could not create bucket {}: {}", bucket, ex.getMessage());
            }
        } catch (Exception e) {
            log.warn("Error checking bucket {}: {}", bucket, e.getMessage());
        }
    }

    @Override
    public StorageFile upload(InputStream data, String fileName, String mimeType) {
        try {
            var key = java.util.UUID.randomUUID() + "-" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            var bytes = data.readAllBytes();
            s3.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(mimeType)
                    .build(),
                RequestBody.fromBytes(bytes));
            log.info("Uploaded {} -> s3://{}/{} ({} bytes)", fileName, bucket, key, bytes.length);
            return new StorageFile(key, fileName, mimeType, bytes.length, providerName);
        } catch (Exception e) {
            throw new RuntimeException("S3 upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String fileId) {
        try {
            var response = s3.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileId)
                .build());
            return response;
        } catch (Exception e) {
            throw new RuntimeException("S3 download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileId) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fileId)
                .build());
            log.info("Deleted s3://{}/{}", bucket, fileId);
        } catch (Exception e) {
            log.warn("S3 delete failed for {}: {}", fileId, e.getMessage());
        }
    }

    @Override
    public boolean exists(String fileId) {
        try {
            s3.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(fileId)
                .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public String getSignedUrl(String fileId, Duration expiry) {
        try {
            var getReq = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileId)
                .build();
            var presignReq = software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(getReq)
                .build();
            var presigned = presigner.presignGetObject(presignReq);
            return presigned.url().toString();
        } catch (Exception e) {
            log.warn("Failed to generate signed URL for {}: {}", fileId, e.getMessage());
            return null;
        }
    }

    @Override
    public FileMetadata getMetadata(String fileId) {
        try {
            var response = s3.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(fileId)
                .build());
            return new FileMetadata(fileId, fileId, response.contentType(),
                response.contentLength(), response.lastModified(), response.eTag());
        } catch (Exception e) {
            throw new RuntimeException("S3 metadata failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() { return providerName; }

    @Override
    public List<StorageFile> listFiles(String prefix, int maxKeys) {
        try {
            var req = ListObjectsV2Request.builder()
                .bucket(bucket)
                .maxKeys(maxKeys)
                .prefix(prefix != null ? prefix : "")
                .build();
            var result = s3.listObjectsV2(req);
            var files = new ArrayList<StorageFile>();
            for (var obj : result.contents()) {
                String mime = obj.key().toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp)") ? "image/*" : "application/octet-stream";
                files.add(new StorageFile(obj.key(), obj.key(), mime, obj.size(), providerName));
            }
            return files;
        } catch (Exception e) {
            log.warn("listFiles S3 fallit: {}", e.getMessage());
            return List.of();
        }
    }
}
