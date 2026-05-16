package com.amg.digitalitzacio.backup.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "app.backup.provider", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class GcsMockClient implements GcsClient {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public String upload(String bucket, String path, byte[] data, String contentType) {
        var key = bucket + "/" + path;
        storage.put(key, data);
        log.info("[MOCK GCS] Uploaded {} bytes to gs://{}/{}", data.length, bucket, path);
        return "gs://" + bucket + "/" + path;
    }

    @Override
    public byte[] download(String bucket, String path) {
        var data = storage.get(bucket + "/" + path);
        if (data == null) {
            log.warn("[MOCK GCS] File not found: gs://{}/{}", bucket, path);
            return new byte[0];
        }
        return data;
    }

    @Override
    public boolean delete(String bucket, String path) {
        var key = bucket + "/" + path;
        var removed = storage.remove(key) != null;
        if (removed) {
            log.info("[MOCK GCS] Deleted gs://{}/{}", bucket, path);
        }
        return removed;
    }

    @Override
    public boolean exists(String bucket, String path) {
        return storage.containsKey(bucket + "/" + path);
    }

    @Override
    public long size(String bucket, String path) {
        var data = storage.get(bucket + "/" + path);
        return data != null ? data.length : 0;
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}
