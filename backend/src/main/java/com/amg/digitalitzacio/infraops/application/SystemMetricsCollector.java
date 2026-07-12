package com.amg.digitalitzacio.infraops.application;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemMetricsCollector {

    private final DataSource dataSource;

    public ServerMetrics collect() {
        var osMxBean = ManagementFactory.getOperatingSystemMXBean();

        // CPU
        double cpuPercent = -1;
        long totalRamBytes = -1;
        long freeRamBytes = -1;
        long totalSwapBytes = -1;
        long freeSwapBytes = -1;
        if (osMxBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            cpuPercent = sunOs.getCpuLoad() * 100;
            totalRamBytes = sunOs.getTotalMemorySize();
            freeRamBytes = sunOs.getFreeMemorySize();
            totalSwapBytes = sunOs.getTotalSwapSpaceSize();
            freeSwapBytes = sunOs.getFreeSwapSpaceSize();
        }

        // Disc
        var root = new File("/");
        long totalDiskBytes = root.getTotalSpace();
        long freeDiskBytes = root.getFreeSpace();

        // DB connections via HikariCP
        int activeConn = 0;
        int maxConn = 10;
        if (dataSource instanceof HikariDataSource hikari) {
            var pool = hikari.getHikariPoolMXBean();
            if (pool != null) {
                activeConn = pool.getActiveConnections();
            }
            maxConn = hikari.getMaximumPoolSize();
        }

        long ramUsedBytes = totalRamBytes > 0 ? totalRamBytes - freeRamBytes : -1;
        double ramPercent = totalRamBytes > 0 ? (double) ramUsedBytes / totalRamBytes * 100 : -1;
        double diskPercent = totalDiskBytes > 0 ? (double) (totalDiskBytes - freeDiskBytes) / totalDiskBytes * 100 : -1;
        double dbPercent = maxConn > 0 ? (double) activeConn / maxConn * 100 : 0;

        return new ServerMetrics(
                Math.max(0, cpuPercent),
                Math.max(0, ramPercent),
                Math.max(0, diskPercent),
                ramUsedBytes / (1024 * 1024),
                totalRamBytes / (1024 * 1024),
                (totalDiskBytes - freeDiskBytes) / (1024 * 1024 * 1024),
                totalDiskBytes / (1024 * 1024 * 1024),
                activeConn,
                maxConn,
                dbPercent,
                totalSwapBytes >= 0 ? totalSwapBytes / (1024 * 1024) : -1,
                (totalSwapBytes > 0 && freeSwapBytes >= 0) ? (totalSwapBytes - freeSwapBytes) / (1024 * 1024) : 0
        );
    }

    public record ServerMetrics(
            double cpuPercent,
            double ramPercent,
            double diskPercent,
            long ramUsedMb,
            long ramTotalMb,
            long diskUsedGb,
            long diskTotalGb,
            int dbActiveConnections,
            int dbMaxConnections,
            double dbConnectionPercent,
            long swapTotalMb,
            long swapUsedMb
    ) {}
}
