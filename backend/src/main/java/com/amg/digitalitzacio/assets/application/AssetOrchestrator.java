package com.amg.digitalitzacio.assets.application;

import com.amg.digitalitzacio.assets.api.dto.AssetResponse;
import com.amg.digitalitzacio.assets.api.dto.AssetStatsResponse;
import com.amg.digitalitzacio.assets.config.StorageConfig;
import com.amg.digitalitzacio.assets.domain.Asset;
import com.amg.digitalitzacio.assets.domain.AssetRepository;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetOrchestrator implements AssetService {

    private final AssetRepository assetRepository;
    private final StorageConfig storageConfig;
    private final SystemConfigService systemConfig;

    private static final long DEFAULT_QUOTA_BYTES = 500L * 1024 * 1024; // 500 MB

    private static final List<String> RASTER_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/avif", "image/gif", "image/svg+xml",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Override
    @Transactional
    public AssetResponse upload(MultipartFile file, UUID tenantId) {
        validateFile(file);
        enforceQuota(tenantId, file.getSize());

        var assetId = UUID.randomUUID();
        var extension = extractExtension(Objects.requireNonNull(file.getOriginalFilename()));
        var storageFilename = assetId + "." + extension;

        var storageDir = Path.of(storageConfig.getPath(), tenantId.toString());
        var thumbsDir = storageDir.resolve("thumbs");
        var targetPath = storageDir.resolve(storageFilename);

        var mimeType = file.getContentType();
        var url = "/api/v1/assets/" + assetId + "/file";
        var relativePath = tenantId + "/" + storageFilename;

        Integer width = null;
        Integer height = null;
        String thumbnailPath = null;
        String thumbnailUrl = null;

        try {
            Files.createDirectories(storageDir);
            Files.createDirectories(thumbsDir);

            try (var is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            if (mimeType != null && RASTER_IMAGE_TYPES.contains(mimeType)) {
                var dimensions = getImageDimensions(targetPath);
                if (dimensions != null) {
                    width = dimensions[0];
                    height = dimensions[1];
                }
                var thumbFilename = generateThumbnail(targetPath, thumbsDir, storageFilename, mimeType);
                if (thumbFilename != null) {
                    thumbnailPath = tenantId + "/thumbs/" + thumbFilename;
                    thumbnailUrl = "/api/v1/assets/" + assetId + "/thumbnail";
                }
            }

            var asset = Asset.builder()
                    .id(assetId)
                    .tenantId(tenantId)
                    .originalName(file.getOriginalFilename())
                    .mimeType(mimeType)
                    .size(file.getSize())
                    .width(width)
                    .height(height)
                    .storagePath(relativePath)
                    .thumbnailPath(thumbnailPath)
                    .url(url)
                    .thumbnailUrl(thumbnailUrl)
                    .isActive(true)
                    .build();

            assetRepository.save(asset);
            log.info("Asset uploaded: {} ({}, {} bytes) for tenant {}", assetId, mimeType, file.getSize(), tenantId);

            return toResponse(asset);
        } catch (IOException e) {
            try { Files.deleteIfExists(targetPath); } catch (IOException ignored) {}
            throw new RuntimeException("Error storing file: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponse> listByTenant(UUID tenantId) {
        return assetRepository.findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource serveFile(UUID assetId) {
        var asset = findActive(assetId);
        var filePath = Path.of(storageConfig.getPath(), asset.getStoragePath());
        validateFileExists(filePath);
        return new FileSystemResource(filePath);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource serveThumbnail(UUID assetId) {
        var asset = findActive(assetId);
        if (asset.getThumbnailPath() == null) {
            throw new ResourceNotFoundException("Thumbnail not found for asset: " + assetId);
        }
        var filePath = Path.of(storageConfig.getPath(), asset.getThumbnailPath());
        validateFileExists(filePath);
        return new FileSystemResource(filePath);
    }

    @Override
    @Transactional
    public void delete(UUID assetId, UUID principalTenantId, String principalRole) {
        var asset = findActive(assetId);

        if ("CLIENT".equals(principalRole) && !asset.getTenantId().equals(principalTenantId)) {
            throw new ResourceNotFoundException("Asset not found: " + assetId);
        }

        asset.setIsActive(false);
        assetRepository.save(asset);
        log.info("Asset logically deleted: {} (tenant {})", assetId, asset.getTenantId());

        // Clean up physical files
        try {
            var storagePath = Path.of(storageConfig.getPath(), asset.getStoragePath());
            Files.deleteIfExists(storagePath);
            if (asset.getThumbnailPath() != null) {
                var thumbPath = Path.of(storageConfig.getPath(), asset.getThumbnailPath());
                Files.deleteIfExists(thumbPath);
            }
        } catch (IOException e) {
            log.warn("Could not delete physical file for asset {}: {}", assetId, e.getMessage());
        }
    }

    // --- Private helpers ---

    private Asset findActive(UUID id) {
        return assetRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > storageConfig.getMaxFileSize()) {
            throw new IllegalArgumentException("File exceeds maximum size of " + storageConfig.getMaxFileSize() + " bytes");
        }

        var mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("File type not allowed: " + mimeType);
        }

        // Validate magic bytes
        try (var is = file.getInputStream()) {
            if (!validateMagicBytes(is, mimeType)) {
                throw new IllegalArgumentException("File content does not match declared type: " + mimeType);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error validating file", e);
        }

        var filename = Objects.requireNonNull(file.getOriginalFilename());
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AssetStatsResponse getStats(UUID tenantId) {
        long usedBytes = assetRepository.sumSizeByTenantId(tenantId);
        long fileCount = assetRepository.countByTenantIdAndIsActiveTrue(tenantId);
        long quotaBytes = resolveQuotaBytes();
        return new AssetStatsResponse(usedBytes, quotaBytes, fileCount);
    }

    private void enforceQuota(UUID tenantId, long incomingSize) {
        long usedBytes = assetRepository.sumSizeByTenantId(tenantId);
        long quotaBytes = resolveQuotaBytes();
        if (usedBytes + incomingSize > quotaBytes) {
            throw new IllegalStateException(
                "Quota d'emmagatzematge superada. Usats: " + usedBytes + " bytes, quota: " + quotaBytes + " bytes.");
        }
    }

    private long resolveQuotaBytes() {
        var raw = systemConfig.get("ASSETS_QUOTA_MB");
        if (raw != null && !raw.isBlank()) {
            try { return Long.parseLong(raw.trim()) * 1024 * 1024; } catch (NumberFormatException ignored) {}
        }
        return DEFAULT_QUOTA_BYTES;
    }

    private boolean validateMagicBytes(InputStream is, String mimeType) throws IOException {
        var magicBytes = new byte[8];
        var bytesRead = is.read(magicBytes, 0, 8);
        if (bytesRead < 4) return false;

        return switch (mimeType) {
            case "image/jpeg" -> bytesRead >= 3 && magicBytes[0] == (byte) 0xFF && magicBytes[1] == (byte) 0xD8;
            case "image/png" -> bytesRead >= 8 && magicBytes[0] == (byte) 0x89 && magicBytes[1] == 0x50;
            case "image/gif" -> bytesRead >= 6 && magicBytes[0] == 0x47 && magicBytes[1] == 0x49;
            case "image/webp" -> bytesRead >= 4 && magicBytes[0] == 0x52 && magicBytes[1] == 0x49;
            case "image/avif" -> true;
            case "image/svg+xml" -> true;
            // PDF: %PDF
            case "application/pdf" -> bytesRead >= 4 && magicBytes[0] == 0x25 && magicBytes[1] == 0x50;
            // DOCX/XLSX/DOC: ZIP (PK) or OLE2 (D0 CF)
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                     bytesRead >= 2 && magicBytes[0] == 0x50 && magicBytes[1] == 0x4B;
            case "application/msword", "application/vnd.ms-excel" ->
                     bytesRead >= 2 && ((magicBytes[0] == (byte) 0xD0 && magicBytes[1] == (byte) 0xCF)
                             || (magicBytes[0] == 0x50 && magicBytes[1] == 0x4B));
            default -> false;
        };
    }

    private String generateThumbnail(Path sourcePath, Path thumbsDir, String filename, String mimeType) throws IOException {
        var originalImage = ImageIO.read(sourcePath.toFile());
        if (originalImage == null) return null;

        var tw = storageConfig.getThumbnailWidth();
        var th = storageConfig.getThumbnailHeight();

        var thumbnail = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
        var g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, tw, th, null);
        g2d.dispose();

        var thumbFilename = filename.replace(".", "_" + tw + "x" + th + ".");
        var thumbPath = thumbsDir.resolve(thumbFilename);

        var formatName = switch (mimeType) {
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            default -> "jpg";
        };

        ImageIO.write(thumbnail, formatName, thumbPath.toFile());
        return thumbFilename;
    }

    private int[] getImageDimensions(Path path) throws IOException {
        var image = ImageIO.read(path.toFile());
        if (image == null) return null;
        return new int[]{image.getWidth(), image.getHeight()};
    }

    private String extractExtension(String filename) {
        var dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "bin";
        return filename.substring(dot + 1).toLowerCase();
    }

    private void validateFileExists(Path path) {
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("File not found on disk: " + path.getFileName());
        }
    }

    private AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getOriginalName(),
                asset.getMimeType(),
                asset.getSize(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getUrl(),
                asset.getThumbnailUrl(),
                asset.getCreatedAt()
        );
    }
}
