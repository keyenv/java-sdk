package dev.keyenv;

import dev.keyenv.types.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that run against the live KeyEnv API.
 * Requires KEYENV_SERVICE_TOKEN and KEYENV_PROJECT_ID environment variables.
 */
@EnabledIfEnvironmentVariable(named = "KEYENV_SERVICE_TOKEN", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    private static KeyEnv client;
    private static String projectId;
    private static String environment = "development";
    private static String testSecretKey;

    @BeforeAll
    static void setUp() {
        String token = System.getenv("KEYENV_SERVICE_TOKEN");
        projectId = System.getenv("KEYENV_PROJECT_ID");

        assertNotNull(token, "KEYENV_SERVICE_TOKEN must be set");
        assertNotNull(projectId, "KEYENV_PROJECT_ID must be set");

        client = KeyEnv.create(token);
        testSecretKey = "TEST_INTEGRATION_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @AfterAll
    static void tearDown() {
        // Clean up test secret
        if (client != null && testSecretKey != null) {
            try {
                client.deleteSecret(projectId, environment, testSecretKey);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("listProjects() returns projects")
    void listProjects() {
        List<Project> projects = client.listProjects();
        assertNotNull(projects);
        assertFalse(projects.isEmpty(), "Should have at least one project");
    }

    @Test
    @Order(2)
    @DisplayName("getProject() returns project details")
    void getProject() {
        Project project = client.getProject(projectId);
        assertNotNull(project);
        assertEquals(projectId, project.getId());
        assertNotNull(project.getName());
    }

    @Test
    @Order(3)
    @DisplayName("listEnvironments() returns environments")
    void listEnvironments() {
        List<Environment> environments = client.listEnvironments(projectId);
        assertNotNull(environments);
        assertFalse(environments.isEmpty(), "Should have at least one environment");

        boolean hasDevEnv = environments.stream()
            .anyMatch(e -> "development".equals(e.getName()) || "dev".equals(e.getName()));
        assertTrue(hasDevEnv, "Should have a development environment");
    }

    @Test
    @Order(4)
    @DisplayName("setSecret() creates a new secret")
    void setSecret() {
        String testValue = "test-value-" + System.currentTimeMillis();

        Secret secret = client.setSecret(projectId, environment, testSecretKey, testValue);

        assertNotNull(secret);
        assertEquals(testSecretKey, secret.getKey());
    }

    @Test
    @Order(5)
    @DisplayName("getSecret() retrieves the created secret")
    void getSecret() {
        SecretWithValue secret = client.getSecret(projectId, environment, testSecretKey);

        assertNotNull(secret);
        assertEquals(testSecretKey, secret.getKey());
        assertNotNull(secret.getValue());
        assertTrue(secret.getValue().startsWith("test-value-"));
    }

    @Test
    @Order(6)
    @DisplayName("getSecrets() returns all secrets for environment")
    void getSecrets() {
        List<SecretWithValue> secrets = client.getSecrets(projectId, environment);

        assertNotNull(secrets);

        boolean hasTestSecret = secrets.stream()
            .anyMatch(s -> testSecretKey.equals(s.getKey()));
        assertTrue(hasTestSecret, "Should contain our test secret");
    }

    @Test
    @Order(7)
    @DisplayName("setSecret() updates existing secret")
    void updateSecret() {
        String updatedValue = "updated-value-" + System.currentTimeMillis();

        Secret secret = client.setSecret(projectId, environment, testSecretKey, updatedValue);

        assertNotNull(secret);

        // Verify update
        client.clearCache(projectId, environment);
        SecretWithValue retrieved = client.getSecret(projectId, environment, testSecretKey);
        assertEquals(updatedValue, retrieved.getValue());
    }

    @Test
    @Order(8)
    @DisplayName("generateEnvFile() generates valid .env content")
    void generateEnvFile() {
        String envContent = client.generateEnvFile(projectId, environment);

        assertNotNull(envContent);
        assertTrue(envContent.contains(testSecretKey + "="));
    }

    @Test
    @Order(9)
    @DisplayName("deleteSecret() removes the secret")
    void deleteSecret() {
        assertDoesNotThrow(() -> client.deleteSecret(projectId, environment, testSecretKey));

        // Verify deletion
        client.clearCache(projectId, environment);
        assertThrows(KeyEnvException.class, () ->
            client.getSecret(projectId, environment, testSecretKey)
        );

        // Mark as cleaned up so AfterAll doesn't try again
        testSecretKey = null;
    }
}
