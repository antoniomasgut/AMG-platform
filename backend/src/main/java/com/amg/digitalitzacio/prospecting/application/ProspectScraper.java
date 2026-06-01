package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.domain.Prospect;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProspectScraper {

    List<Prospect> search(String sector, String location, Map<String, Object> params, UUID campaignId);

    record ProspectEnrichment(String email, String website, String instagram, Boolean hasWebsite, Boolean hasWhatsapp) {}

    ProspectEnrichment enrich(String name, String website, String email);

    record PlaceDetails(String phone, String website, String city, String postalCode, String description) {}

    PlaceDetails fetchDetails(String googlePlaceId);
}
