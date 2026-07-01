package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Importa prospects des d'un fitxer CSV.
 * Format esperat: name,phone,email,website,address,city (primera fila = capçaleres).
 * Màxim 500 files per importació. Deduplicació per (campaignId+phone) i (campaignId+email).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProspectCsvImportService {

    private static final int MAX_ROWS = 500;

    private final ProspectRepository prospectRepository;
    private final ProspectCampaignRepository campaignRepository;

    @Transactional
    public Map<String, Integer> importCsv(UUID campaignId, MultipartFile file) {
        campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));

        List<String[]> rows = parseCsv(file);
        if (rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("El CSV supera el màxim de " + MAX_ROWS + " files (" + rows.size() + " detectades)");
        }

        int imported = 0;
        int skipped = 0;

        for (String[] row : rows) {
            try {
                String name    = get(row, 0);
                String phone   = get(row, 1);
                String email   = get(row, 2);
                String website = get(row, 3);
                String address = get(row, 4);
                String city    = get(row, 5);

                if (name == null || name.isBlank()) {
                    skipped++;
                    continue;
                }

                // Deduplicació per telèfon o email dins la mateixa campanya
                if (phone != null && !phone.isBlank() && prospectRepository.existsByCampaignIdAndPhone(campaignId, phone)) {
                    skipped++;
                    continue;
                }
                if (email != null && !email.isBlank() && prospectRepository.existsByCampaignIdAndEmail(campaignId, email)) {
                    skipped++;
                    continue;
                }

                var prospect = Prospect.builder()
                        .campaignId(campaignId)
                        .name(name)
                        .phone(phone)
                        .email(email)
                        .website(website != null && !website.isBlank() ? website : null)
                        .hasWebsite(website != null && !website.isBlank())
                        .address(address)
                        .city(city)
                        .source(ProspectSource.MANUAL)
                        .status(ProspectStatus.NEW)
                        .build();

                prospectRepository.save(prospect);
                imported++;
            } catch (Exception e) {
                log.warn("Error important fila CSV: {}", e.getMessage());
                skipped++;
            }
        }

        log.info("CSV import per campanya {}: {} importats, {} omesos", campaignId, imported, skipped);
        return Map.of("imported", imported, "skipped", skipped);
    }

    private List<String[]> parseCsv(MultipartFile file) {
        var rows = new ArrayList<String[]>();
        try (var reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                // Salta la fila de capçaleres
                if (first) {
                    first = false;
                    if (line.toLowerCase().contains("name") || line.toLowerCase().contains("nom")) continue;
                }
                rows.add(splitCsvLine(line));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error llegint el CSV: " + e.getMessage());
        }
        return rows;
    }

    /** Split simple per comes, respectant cometes dobles. */
    private String[] splitCsvLine(String line) {
        var result = new ArrayList<String>();
        var sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().strip());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().strip());
        return result.toArray(new String[0]);
    }

    private String get(String[] row, int idx) {
        if (row == null || idx >= row.length) return null;
        String val = row[idx];
        return (val == null || val.isBlank()) ? null : val.strip();
    }
}
