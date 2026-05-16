package com.amg.digitalitzacio.infraops.api.dto;

public record MetricStatusDto(double percent, String status) {
    public static MetricStatusDto of(double percent, double warn, double crit) {
        String status = percent >= crit ? "CRITICAL" : percent >= warn ? "WARNING" : "OK";
        return new MetricStatusDto(Math.round(percent * 10.0) / 10.0, status);
    }
}
