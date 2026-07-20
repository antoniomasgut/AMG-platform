package com.amg.digitalitzacio.social.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Afegeix paràmetres UTM als enllaços dels captions de posts socials,
 * per poder atribuir visites i leads a cada xarxa (P2 · Mòdul 52/55).
 *
 * Regles: només toca URLs http(s) que encara no portin utm_source;
 * respecta la puntuació final típica de text (parèntesi, punt, coma...).
 */
public final class UtmTagger {

    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"]+");
    private static final String TRAILING_PUNCT = ".,;:!?)]}»";

    private UtmTagger() {}

    /**
     * @param caption text del post (pot ser null)
     * @param network INSTAGRAM | FACEBOOK | GOOGLE_BUSINESS | LINKEDIN
     * @return caption amb UTM afegits als enllaços
     */
    public static String tag(String caption, String network) {
        if (caption == null || caption.isBlank()) return caption;
        String source = switch (network) {
            case "INSTAGRAM"       -> "instagram";
            case "FACEBOOK"        -> "facebook";
            case "GOOGLE_BUSINESS" -> "google_business";
            case "LINKEDIN"        -> "linkedin";
            default                -> null;
        };
        if (source == null) return caption;

        Matcher m = URL.matcher(caption);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String url = m.group();
            // Separa la puntuació final que no forma part de la URL
            int end = url.length();
            while (end > 0 && TRAILING_PUNCT.indexOf(url.charAt(end - 1)) >= 0) end--;
            String clean = url.substring(0, end);
            String punct = url.substring(end);

            String tagged = clean.contains("utm_source=")
                ? clean
                : clean + (clean.contains("?") ? "&" : "?")
                    + "utm_source=" + source + "&utm_medium=social&utm_campaign=amg_social";

            m.appendReplacement(sb, Matcher.quoteReplacement(tagged + punct));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
