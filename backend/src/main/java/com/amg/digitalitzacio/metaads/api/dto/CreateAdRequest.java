package com.amg.digitalitzacio.metaads.api.dto;

import java.util.UUID;

public record CreateAdRequest(
    String name,
    UUID creativeId,
    String headline,
    String body,
    String description,
    String callToAction,
    String linkUrl,
    String metaImageHash,
    UUID imageAssetId
) {}
