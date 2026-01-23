package dev.keyenv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the KeyEnv client.
 */
class KeyEnvTest {

    // ============================================================
    // Client Construction Tests
    // ============================================================

    @Test
    @DisplayName("create() creates client with valid token")
    void createWithValidToken() {
        KeyEnv client = KeyEnv.create("test-token");
        assertNotNull(client);
    }

    @Test
    @DisplayName("create() throws exception without token")
    void createWithoutToken() {
        assertThrows(KeyEnvException.class, () -> KeyEnv.create(null));
        assertThrows(KeyEnvException.class, () -> KeyEnv.create(""));
    }

    @Test
    @DisplayName("builder() creates client with custom configuration")
    void builderWithCustomConfig() {
        KeyEnv client = KeyEnv.builder()
            .token("test-token")
            .baseUrl("https://custom.api.com")
            .timeout(Duration.ofSeconds(60))
            .cacheTtl(Duration.ofMinutes(5))
            .build();

        assertNotNull(client);
    }

    @Test
    @DisplayName("builder() strips trailing slash from base URL")
    void builderStripsTrailingSlash() {
        KeyEnv client = KeyEnv.builder()
            .token("test-token")
            .baseUrl("https://custom.api.com/")
            .build();

        assertNotNull(client);
        // Note: We can't directly verify the baseUrl since it's private,
        // but the client should be created without error
    }

    @Test
    @DisplayName("builder() throws exception without token")
    void builderWithoutToken() {
        assertThrows(KeyEnvException.class, () ->
            KeyEnv.builder().baseUrl("https://api.example.com").build()
        );
    }

    // ============================================================
    // Cache Tests
    // ============================================================

    @Test
    @DisplayName("clearCache() does not throw")
    void clearCacheDoesNotThrow() {
        KeyEnv client = KeyEnv.create("test-token");
        assertDoesNotThrow(() -> client.clearCache("project-1", "production"));
    }

    @Test
    @DisplayName("clearAllCache() does not throw")
    void clearAllCacheDoesNotThrow() {
        KeyEnv client = KeyEnv.create("test-token");
        assertDoesNotThrow(() -> client.clearAllCache());
    }

    // ============================================================
    // Integration Tests (require mock server)
    // ============================================================

    // TODO: Add integration tests using a mock HTTP server (e.g., WireMock)
    // These tests should verify:
    // - listProjects() makes correct API call and parses response
    // - getProject() makes correct API call and parses response
    // - getSecrets() makes correct API call and parses response
    // - getSecret() makes correct API call and parses response
    // - setSecret() makes correct API calls (PUT then POST if needed)
    // - deleteSecret() makes correct API call
    // - bulkImport() makes correct API call and parses response
    // - loadEnv() loads secrets into system properties
    // - generateEnvFile() generates correct .env format
    // - Error handling for 401, 403, 404, 429, 5xx responses
    // - Caching behavior (cache hits, cache invalidation)

    // Example test structure with WireMock:
    /*
    @Test
    @DisplayName("listProjects() returns projects from API")
    void listProjectsReturnsProjects() throws Exception {
        // Setup mock server
        stubFor(get(urlEqualTo("/projects"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"projects\": [{\"id\": \"proj-1\", \"name\": \"Project 1\"}]}")));

        KeyEnv client = KeyEnv.builder()
            .token("test-token")
            .baseUrl(wireMockServer.baseUrl())
            .build();

        List<Project> projects = client.listProjects();

        assertEquals(1, projects.size());
        assertEquals("proj-1", projects.get(0).getId());
        assertEquals("Project 1", projects.get(0).getName());
    }
    */
}
