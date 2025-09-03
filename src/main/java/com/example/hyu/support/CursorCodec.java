package com.example.hyu.support;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public final class CursorCodec {
    /**
 * Private constructor to prevent instantiation of this utility class.
 *
 * <p>All members are static; no instances are required or supported.</p>
 */
private CursorCodec() {}

    /**
     * Encodes a (publishedAt, id) pair into a URL-safe Base64 cursor string.
     *
     * The raw payload is "<epochMillis>:<id>" (UTF-8) and is encoded using a URL-safe
     * Base64 encoder without padding.
     *
     * @param publishedAt the instant to encode as epoch milliseconds; must not be null
     * @param id the identifier to include in the payload (may be null, in which case the string "null" is used)
     * @return a URL-safe Base64 string representing the encoded cursor
     * @throws NullPointerException if {@code publishedAt} is null
     */
    public static String encode(Instant publishedAt, Long id) {
        String raw = publishedAt.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a URL-safe Base64 cursor into its constituent published timestamp and id.
     *
     * <p>Expects the cursor to be a URL-safe Base64 (no-padding) encoding of the string
     * "<epochMillis>:<id>" where <epochMillis> is milliseconds since the epoch and <id> is a
     * decimal long. If {@code cursor} is {@code null} or blank, this method returns {@code null}.
     *
     * @param cursor the URL-safe Base64 encoded cursor string (may be {@code null} or blank)
     * @return a {@link Parsed} containing the decoded {@link java.time.Instant publishedAt} and {@link Long id},
     *         or {@code null} if {@code cursor} is {@code null} or blank
     */
    public static Parsed decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] sp = raw.split(":");
        return new Parsed(Instant.ofEpochMilli(Long.parseLong(sp[0])), Long.parseLong(sp[1]));
    }

    public record Parsed(Instant publishedAt, Long id) {}
}