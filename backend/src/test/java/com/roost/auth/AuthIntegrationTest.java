package com.roost.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roost.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack auth: real Postgres (Flyway V3 + {@code ddl-auto: validate}), the
 * bootstrap invite seeded from config, Argon2 hashing, and the JWT
 * resource-server chain. Exercises the register → login happy path plus the
 * security-relevant failure modes end to end.
 *
 * <p>The bootstrap invite is single-use, so exactly one test redeems it; the
 * others cover paths that never consume an invite, keeping the shared context's
 * state independent of test order.
 */
@SpringBootTest(properties = {
    "roost.security.jwt-secret=integration-test-jwt-secret-at-least-32-bytes",
    "roost.security.bootstrap-invite-code=bootstrap-secret-code",
    "REDIS_PASSWORD=test-only-redis-password",
    // Placeholder only; the Postgres Testcontainer supplies the real connection.
    "POSTGRES_PASSWORD=test-only-postgres-password"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private static String registerBody(String username, String email, String inviteCode) {
        return """
            {"username":"%s","email":"%s","password":"password123",
             "displayName":"Root","inviteCode":"%s"}
            """.formatted(username, email, inviteCode);
    }

    @Test
    void redeemsBootstrapInviteThenLoginsAndBlocksReuse() throws Exception {
        // Register the first account with the seeded bootstrap invite -> ADMIN.
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("root", "root@roost.dev", "bootstrap-secret-code")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("root"))
            .andExpect(jsonPath("$.role").value("ADMIN"));

        // Log in and receive a bearer token.
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"root","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // The single-use bootstrap invite is now spent.
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("second", "second@roost.dev", "bootstrap-secret-code")))
            .andExpect(status().isBadRequest());

        // Wrong password is a generic 401.
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"root","password":"wrong-password"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationWithoutInviteIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("noinvite", "noinvite@roost.dev", "")))
            .andExpect(status().isForbidden());
    }

    @Test
    void registrationWithUnknownInviteIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("bad", "bad@roost.dev", "does-not-exist")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void invalidRequestBodyIsRejected() throws Exception {
        // Password too short trips bean validation before any business logic.
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"x","email":"not-an-email","password":"short",
                     "displayName":"","inviteCode":"whatever"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void protectedRouteRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/servers"))
            .andExpect(status().isUnauthorized());
    }
}
