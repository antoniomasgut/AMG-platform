package com.amg.digitalitzacio.assets.config;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.storage")
@Getter @Setter
@Validated
public class StorageConfig {

    private String path = "/data/assets";

    @Positive
    private long maxFileSize = 5_242_880; // 5 MB

    @Positive
    private int thumbnailWidth = 200;

    @Positive
    private int thumbnailHeight = 200;

    private List<String> allowedTypes = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/avif",
            "image/gif",
            "image/svg+xml"
    );
}
