package com.roost.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed binding for the {@code roost.security.*} configuration. Grouping the
 * secret, token lifetime, and bootstrap code in one validated record keeps the
 * wiring in one place and lets the app fail fast at startup on missing/invalid
 * config rather than at first request.
 *
 * @param jwtSecret HMAC signing secret; must be at least 32 bytes for HS256.
 * @param jwtTtl access-token lifetime.
 * @param bootstrapInviteCode optional seed for the first invite; may be blank
 *     when at least one user already exists (see the bootstrap initializer).
 */
@Validated
@ConfigurationProperties(prefix = "roost.security")
public record RoostSecurityProperties(
    @NotBlank String jwtSecret,
    Duration jwtTtl,
    String bootstrapInviteCode) {

    /** HS256 needs a key of at least 256 bits; reject anything shorter. */
    private static final int MIN_SECRET_BYTES = 32;

    public RoostSecurityProperties {
        if (jwtSecret != null && jwtSecret.getBytes().length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "roost.security.jwt-secret must be at least " + MIN_SECRET_BYTES
                    + " bytes (256 bits) for HS256");
        }
        if (jwtTtl == null) {
            jwtTtl = Duration.ofHours(1);
        }
    }
}
