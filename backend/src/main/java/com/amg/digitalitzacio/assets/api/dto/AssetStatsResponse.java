package com.amg.digitalitzacio.assets.api.dto;

public record AssetStatsResponse(
        long usedBytes,
        long quotaBytes,
        long fileCount
) {}
