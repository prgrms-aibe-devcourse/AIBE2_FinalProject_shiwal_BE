package com.example.hyu.support;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public final class CursorCodec {
    private CursorCodec() {}

    public static String encode(Instant publishedAt, Long id) {
        String raw = publishedAt.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Parsed decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] sp = raw.split(":");
        return new Parsed(Instant.ofEpochMilli(Long.parseLong(sp[0])), Long.parseLong(sp[1]));
    }

    public record Parsed(Instant publishedAt, Long id) {}
}