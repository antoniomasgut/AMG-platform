package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.domain.Prospect;
import com.amg.digitalitzacio.prospecting.domain.ProspectSource;
import com.amg.digitalitzacio.prospecting.domain.ProspectStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.prospecting.provider", havingValue = "mock", matchIfMissing = true)
public class MockProspectScraper implements ProspectScraper {

    @Override
    public List<Prospect> search(String sector, String location, Map<String, Object> params, UUID campaignId) {
        var p1 = Prospect.builder()
                .campaignId(campaignId).name("Restaurant Ca'n Pedro").sector(sector)
                .city("Palma").address("Carrer Major 1, Palma")
                .googleRating(new BigDecimal("4.5")).googleReviews(120)
                .hasWebsite(true).status(ProspectStatus.NEW).source(ProspectSource.GOOGLE_MAPS)
                .googlePlaceId("place-1").build();

        var p2 = Prospect.builder()
                .campaignId(campaignId).name("Bar España").sector(sector)
                .city("Inca").address("Plaça Espanya 5, Inca")
                .googleRating(new BigDecimal("3.8")).googleReviews(45)
                .hasWebsite(false).status(ProspectStatus.NEW).source(ProspectSource.GOOGLE_MAPS)
                .googlePlaceId("place-2").build();

        var p3 = Prospect.builder()
                .campaignId(campaignId).name("Pizza Napoli").sector(sector)
                .city("Palma").address("Carrer Sant Miquel 10, Palma")
                .googleRating(new BigDecimal("4.2")).googleReviews(89)
                .hasWebsite(true).hasInstagram(true)
                .status(ProspectStatus.NEW).source(ProspectSource.GOOGLE_MAPS)
                .googlePlaceId("place-3").build();

        return List.of(p1, p2, p3);
    }

    @Override
    public ProspectEnrichment enrich(String name, String website, String email) {
        var domain = website != null
                ? website.replace("https://", "").replace("http://", "").replace("/", "")
                : "example.com";
        return new ProspectEnrichment(
                "contact@" + domain, website,
                "@" + name.toLowerCase().replace(" ", ""),
                true, true
        );
    }

    @Override
    public PlaceDetails fetchDetails(String googlePlaceId) {
        return switch (googlePlaceId) {
            case "place-1" -> new PlaceDetails("+34 971 123 456", "https://canpedro.com", "Palma", "07001", null);
            case "place-2" -> new PlaceDetails("+34 971 234 567", null, "Inca", "07300", null);
            case "place-3" -> new PlaceDetails("+34 971 345 678", "https://pizzanapoli.es", "Palma", "07002", null);
            default -> new PlaceDetails(null, null, null, null, null);
        };
    }
}
