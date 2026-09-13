package com.roost.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Boot-time invariants on the security config: fail fast on bad values. */
class RoostSecurityPropertiesTest {

    private static final String VALID_SECRET = "a-sufficiently-long-hs256-secret-key!!";

    @Test
    void defaultsTtlToOneHourWhenAbsent() {
        RoostSecurityProperties props = new RoostSecurityProperties(VALID_SECRET, null, null);
        assertThat(props.jwtTtl()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new RoostSecurityProperties("too-short", null, null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsZeroTtl() {
        assertThatThrownBy(() -> new RoostSecurityProperties(VALID_SECRET, Duration.ZERO, null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNegativeTtl() {
        assertThatThrownBy(() ->
                new RoostSecurityProperties(VALID_SECRET, Duration.ofMinutes(-5), null))
            .isInstanceOf(IllegalStateException.class);
    }
}
