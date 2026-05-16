package com.amg.digitalitzacio.backup.application;

public interface GcsClient {

    String upload(String bucket, String path, byte[] data, String contentType);

    byte[] download(String bucket, String path);

    boolean delete(String bucket, String path);

    boolean exists(String bucket, String path);

    long size(String bucket, String path);

    boolean isConnected();
}
