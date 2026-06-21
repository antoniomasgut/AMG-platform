package com.amg.digitalitzacio.assets.application;

import com.amg.digitalitzacio.assets.api.dto.AssetResponse;
import com.amg.digitalitzacio.assets.api.dto.AssetStatsResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AssetService {

    AssetResponse upload(MultipartFile file, UUID tenantId);

    List<AssetResponse> listByTenant(UUID tenantId);

    AssetStatsResponse getStats(UUID tenantId);

    AssetResponse getAsset(UUID assetId);

    Resource serveFile(UUID assetId);

    Resource serveThumbnail(UUID assetId);

    void delete(UUID assetId, UUID principalTenantId, String principalRole);
}
