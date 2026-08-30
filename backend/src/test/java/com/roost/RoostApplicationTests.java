package com.roost;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "JWT_SECRET=test-only-secret-not-used-in-production")
class RoostApplicationTests {

    @Test
    void contextLoads() {
        // Boots the full context against a real Postgres (Testcontainers) and
        // runs Flyway; fails if wiring, config, or migrations are broken.
    }
}
