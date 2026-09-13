package com.roost.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates and hashes invite codes. A code is 32 bytes of {@link SecureRandom}
 * entropy, URL-safe Base64 without padding. Because the code is high-entropy and
 * unguessable, a plain SHA-256 hash is enough to store it safely — a slow,
 * salted password KDF (Argon2) is unnecessary here and would only cost latency.
 */
public final class InviteCodes {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final int CODE_BYTES = 32;

    private InviteCodes() {
    }

    /** Returns a fresh, unguessable invite code (the raw secret, shown once). */
    public static String generate() {
        byte[] bytes = new byte[CODE_BYTES];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    /** SHA-256 of the code, lowercase hex (64 chars) — the stored form. */
    public static String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JDK; absence is unrecoverable.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
