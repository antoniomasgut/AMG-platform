package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.google.domain.GoogleModuleConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleBusinessHoursServiceTest {

    @Mock GoogleModuleConfigRepository configRepo;
    @Mock GoogleTokenService tokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private GoogleBusinessHoursService service() {
        return new GoogleBusinessHoursService(configRepo, tokenService, objectMapper);
    }

    private static final LocalDate TODAY = LocalDate.parse("2026-07-08");

    @SuppressWarnings("unchecked")
    private static Map<String, Object> startDateOf(Map<String, Object> period) {
        return (Map<String, Object>) period.get("startDate");
    }

    // ── mergeClosedPeriods ─────────────────────────────────────────────────────

    @Test
    void merge_emptyCurrent_addsOnePeriodPerDay() throws Exception {
        var merged = service().mergeClosedPeriods(null,
                LocalDate.parse("2026-07-14"), LocalDate.parse("2026-07-16"), TODAY);

        assertThat(merged).hasSize(3);
        assertThat(merged.get(0)).containsEntry("closed", true);
        assertThat(startDateOf(merged.get(0)))
                .containsEntry("year", 2026).containsEntry("month", 7).containsEntry("day", 14);
    }

    @Test
    void merge_keepsFutureExistingPeriods() throws Exception {
        var current = objectMapper.readTree("""
            {"specialHours":{"specialHourPeriods":[
              {"startDate":{"year":2026,"month":8,"day":15},"closed":true}
            ]}}""");

        var merged = service().mergeClosedPeriods(current,
                LocalDate.parse("2026-07-20"), LocalDate.parse("2026-07-20"), TODAY);

        assertThat(merged).hasSize(2);
    }

    @Test
    void merge_dropsPastPeriods() throws Exception {
        var current = objectMapper.readTree("""
            {"specialHours":{"specialHourPeriods":[
              {"startDate":{"year":2026,"month":1,"day":6},"closed":true}
            ]}}""");

        var merged = service().mergeClosedPeriods(current,
                LocalDate.parse("2026-07-20"), LocalDate.parse("2026-07-20"), TODAY);

        assertThat(merged).hasSize(1);
        assertThat(startDateOf(merged.get(0))).containsEntry("day", 20);
    }

    @Test
    void merge_replacesOverlappingPeriodWithoutDuplicates() throws Exception {
        var current = objectMapper.readTree("""
            {"specialHours":{"specialHourPeriods":[
              {"startDate":{"year":2026,"month":7,"day":15},"closed":true}
            ]}}""");

        var merged = service().mergeClosedPeriods(current,
                LocalDate.parse("2026-07-14"), LocalDate.parse("2026-07-16"), TODAY);

        assertThat(merged).hasSize(3);
    }

    @Test
    void merge_skipsDaysBeforeToday() throws Exception {
        var merged = service().mergeClosedPeriods(null,
                LocalDate.parse("2026-07-06"), LocalDate.parse("2026-07-09"), TODAY);

        assertThat(merged).hasSize(2); // només 08 i 09
    }

    // ── markClosed gates ───────────────────────────────────────────────────────

    @Test
    void markClosed_noConfig_returnsFalseWithoutHttp() {
        var tenantId = UUID.randomUUID();
        when(configRepo.findById(tenantId)).thenReturn(Optional.empty());

        assertThat(service().markClosed(tenantId, TODAY, TODAY)).isFalse();
        verifyNoInteractions(tokenService);
    }

    @Test
    void markClosed_invalidRange_returnsFalse() {
        var tenantId = UUID.randomUUID();
        var config = new com.amg.digitalitzacio.google.domain.GoogleModuleConfig();
        config.setTenantId(tenantId);
        config.setBusinessEnabled(true);
        config.setBusinessLocationId("123");
        when(configRepo.findById(tenantId)).thenReturn(Optional.of(config));

        assertThat(service().markClosed(tenantId, TODAY, TODAY.minusDays(1))).isFalse();
        verifyNoInteractions(tokenService);
    }
}
