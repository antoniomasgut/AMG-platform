package com.amg.digitalitzacio.metaads.api.dto;

import java.util.List;

public record PublishProgressResponse(
    String campaignStatus,
    List<String> steps,
    String error
) {}
