package com.amg.digitalitzacio.shared.storage;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

public interface StorageProvider {
    StorageFile upload(InputStream data, String fileName, String mimeType);
    InputStream download(String fileId);
    void delete(String fileId);
    boolean exists(String fileId);
    String getSignedUrl(String fileId, Duration expiry);
    FileMetadata getMetadata(String fileId);
    String getProviderName();
    /** P44: llista fitxers (imatges) del bucket, màxim maxKeys. Prefix pot ser null o buit. */
    default List<StorageFile> listFiles(String prefix, int maxKeys) { return List.of(); }
}
