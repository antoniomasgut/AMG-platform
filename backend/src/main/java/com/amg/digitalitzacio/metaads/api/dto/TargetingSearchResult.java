package com.amg.digitalitzacio.metaads.api.dto;

import java.util.List;

public record TargetingSearchResult(
    List<Item> interests,
    List<Item> locations
) {
    public record Item(String id, String name, Long audienceSize) {}
}
