plugins {
    java
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.roost"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web + WebSocket (STOMP) for REST and real-time chat
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Security + JWT (auth, short-lived tokens)
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Persistence: JPA + Flyway (Postgres)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Presence / pub-sub
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Health + metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Config validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Tests: JUnit 5 + Testcontainers (real Postgres)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Testcontainers' bundled docker-java does not auto-negotiate the Docker
    // API version and falls back to an old one (1.32), which recent daemons
    // (Engine 25+/API >= 1.40) reject. Pin a modern, widely supported version.
    // Overridable via -PdockerApiVersion=... for older local daemons.
    systemProperty("api.version", (project.findProperty("dockerApiVersion") ?: "1.41") as String)
}
