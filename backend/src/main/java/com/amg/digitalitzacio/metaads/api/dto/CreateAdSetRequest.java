package com.amg.digitalitzacio.metaads.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CreateAdSetRequest(
    String name,
    BigDecimal dailyBudget,
    String optimizationGoal,
    String billingEvent,
    BigDecimal bidAmount,
    Integer ageMin,
    Integer ageMax,
    String genders,
    List<GeoLocation> geoLocations,
    List<Interest> interests,
    String publisherPlatforms,
    Instant startTime,
    Instant stopTime
) {
    public record GeoLocation(String key, String name, Integer radius, String distanceUnit) {}
    public record Interest(String id, String name) {}
}
