package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.domain.Prospect;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Detecta el millor canal de contacte disponible per a un prospect.
 * Ordre de prioritat: EMAIL > WHATSAPP > FORM > INSTAGRAM > FACEBOOK > LINKEDIN > PHONE
 */
@Service
public class ProspectChannelDetectorService {

    public record ChannelResult(String channel, String target, String reason) {}

    public ChannelResult detect(Prospect p) {
        if (hasValue(p.getEmail()))
            return new ChannelResult("EMAIL", p.getEmail(), "Email trobat a la web o via enriquiment");

        if (Boolean.TRUE.equals(p.getHasWhatsapp()) && hasValue(p.getPhone()))
            return new ChannelResult("WHATSAPP", "https://wa.me/34" + clean(p.getPhone()), "WhatsApp Business detectat");

        if (Boolean.TRUE.equals(p.getHasContactForm()) && hasValue(p.getWebsite()))
            return new ChannelResult("FORM", p.getWebsite(), "Formulari de contacte detectat a la web");

        if (Boolean.TRUE.equals(p.getHasInstagram()) && hasValue(p.getInstagram()))
            return new ChannelResult("INSTAGRAM_DM", "https://instagram.com/" + p.getInstagram(), "Instagram detectat");

        if (Boolean.TRUE.equals(p.getHasFacebook()) && hasValue(p.getFacebookUrl()))
            return new ChannelResult("FACEBOOK", p.getFacebookUrl(), "Facebook detectat");

        if (Boolean.TRUE.equals(p.getHasLinkedin()) && hasValue(p.getLinkedinUrl()))
            return new ChannelResult("LINKEDIN", p.getLinkedinUrl(), "LinkedIn detectat");

        if (hasValue(p.getPhone()))
            return new ChannelResult("PHONE", p.getPhone(), "Telèfon disponible (contacte manual)");

        return new ChannelResult("UNKNOWN", null, "Cap canal disponible — enriquiment necessari");
    }

    public Map<String, String> detectAll(Prospect p) {
        var channels = new LinkedHashMap<String, String>();
        if (hasValue(p.getEmail())) channels.put("EMAIL", p.getEmail());
        if (Boolean.TRUE.equals(p.getHasWhatsapp()) && hasValue(p.getPhone()))
            channels.put("WHATSAPP", "https://wa.me/34" + clean(p.getPhone()));
        if (Boolean.TRUE.equals(p.getHasContactForm()) && hasValue(p.getWebsite()))
            channels.put("FORM", p.getWebsite());
        if (Boolean.TRUE.equals(p.getHasInstagram()) && hasValue(p.getInstagram()))
            channels.put("INSTAGRAM_DM", "https://instagram.com/" + p.getInstagram());
        if (Boolean.TRUE.equals(p.getHasFacebook()) && hasValue(p.getFacebookUrl()))
            channels.put("FACEBOOK", p.getFacebookUrl());
        if (Boolean.TRUE.equals(p.getHasLinkedin()) && hasValue(p.getLinkedinUrl()))
            channels.put("LINKEDIN", p.getLinkedinUrl());
        if (hasValue(p.getPhone())) channels.put("PHONE", p.getPhone());
        return channels;
    }

    private boolean hasValue(String s) { return s != null && !s.isBlank(); }
    private String clean(String phone) { return phone.replaceAll("\\D", ""); }
}
