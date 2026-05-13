package com.amg.digitalitzacio.assets.application;

import com.amg.digitalitzacio.assets.api.dto.AssetResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AssetService {

    AssetResponse upload(MultipartFile file, UUID tenantId);

    List<AssetResponse> listByTenant(UUID tenantId);

    Resource serveFile(UUID assetId);

    Resource serveThumbnail(UUID assetId);

    void delete(UUID assetId, UUID principalTenantId, String principalRole);
}
