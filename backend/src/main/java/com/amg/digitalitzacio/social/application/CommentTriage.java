package com.amg.digitalitzacio.social.application;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Triatge de comentaris (P4 · Mòdul 55): decideix si un comentari mereix
 * notificació al Telegram del tenant o és soroll (emojis sols, reaccions
 * curtes, spam). Determinista i sense cost d'IA — en cas de dubte, notifica.
 */
public final class CommentTriage {

    /** Reaccions d'una sola paraula que no demanen resposta. */
    private static final Set<String> REACTION_WORDS = Set.of(
        "ok", "oki", "okey", "top", "wow", "uau", "like", "nice", "cool",
        "genial", "brutal", "guay", "ole", "olé", "bravo", "yes", "si", "sí",
        "jaja", "jajaja", "haha", "hahaha", "jeje", "hehe", "lol");

    /** Senyals de spam habituals als comentaris de pàgines de negocis. */
    private static final Pattern SPAM = Pattern.compile(
        "(?i)(bitcoin|crypto|forex|trading|invertir|inversió|inversion|guanya\\s+diners|"
        + "gana\\s+dinero|make\\s+money|followers|seguidores\\s+gratis|free\\s+followers|"
        + "préstec|prestamo|loan|dm\\s+me|escríbeme\\s+al|whatsapp\\s*\\+\\d)");

    private static final Pattern URL_ONLY = Pattern.compile("^\\s*https?://\\S+\\s*$");

    private CommentTriage() {}

    /**
     * @return true si el comentari mereix notificar el tenant
     */
    public static boolean worthNotifying(String message) {
        // Sense text (comentari només amb sticker/foto) → no cal resposta
        if (message == null || message.isBlank()) return false;

        String trimmed = message.trim();

        // Només un enllaç, o senyals de spam → fora
        if (URL_ONLY.matcher(trimmed).matches()) return false;
        if (SPAM.matcher(trimmed).find()) return false;

        // Treu emojis, símbols i puntuació; queda només lletres/números
        String textOnly = trimmed.replaceAll("[^\\p{L}\\p{N}\\s]", "").trim();

        // Res de text (emojis sols) → fora
        if (textOnly.isEmpty()) return false;

        // Una sola paraula de reacció ("top", "jajaja"...) → fora
        String[] tokens = textOnly.toLowerCase().split("\\s+");
        if (tokens.length == 1 && REACTION_WORDS.contains(tokens[0])) return false;

        return true;
    }
}
