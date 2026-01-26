package dev.keyenv;

import dev.keyenv.types.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests that run against the live KeyEnv API.
 *
 * <p>These tests are skipped unless the following environment variables are set:
 * <ul>
 *   <li>KEYENV_API_URL - API base URL (e.g., http://localhost:8081/api/v1)</li>
 *   <li>KEYENV_TOKEN - Service token for authentication</li>
 *   <li>KEYENV_PROJECT - Project slug (optional, defaults to "sdk-test")</li>
 * </ul>
 *
 * <p>To run locally against the test API:
 * <pre>
 * make test-infra-up
 * KEYENV_API_URL=http://localhost:8081/api/v1 \
 * KEYENV_TOKEN=env_test_integration_token_12345 \
 * mvn test -Dtest=IntegrationTest
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    private static KeyEnv client;
    private static String projectSlug;
    private static String environment = "development";
    private static String testSecretKey;
    private static String testSecretValue;
    private static boolean secretCreated = false;

    @BeforeAll
    static void checkEnv() {
        String apiUrl = System.getenv("KEYENV_API_URL");
        assumeTrue(apiUrl != null && !apiUrl.isEmpty(), "KEYENV_API_URL not set - skipping integration tests");

        String token = System.getenv("KEYENV_TOKEN");
        assumeTrue(token != null && !token.isEmpty(), "KEYENV_TOKEN not set - skipping integration tests");

        projectSlug = System.getenv("KEYENV_PROJECT");
        if (projectSlug == null || projectSlug.isEmpty()) {
            projectSlug = "sdk-test";
        }

        // Build base URL - remove /api/v1 suffix if present since SDK adds it
        String baseUrl = apiUrl;
        if (baseUrl.endsWith("/api/v1")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 7);
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        client = KeyEnv.builder()
            .token(token)
            .baseUrl(baseUrl)
            .build();

        // Generate unique test key using timestamp to avoid conflicts
        testSecretKey = "JAVA_SDK_TEST_" + System.currentTimeMillis();
        testSecretValue = "test-value-" + System.currentTimeMillis();
    }

    @AfterAll
    static void cleanup() {
        // Clean up test secret if it was created
        if (client != null && secretCreated && testSecretKey != null) {
            try {
                client.deleteSecret(projectSlug, environment, testSecretKey);
                System.out.println("Cleaned up test secret: " + testSecretKey);
            } catch (Exception e) {
                // Ignore cleanup errors - secret may have been deleted by test
                System.out.println("Note: Could not delete test secret during cleanup: " + e.getMessage());
            }
        }
    }

    // ============================================================
    // Project Tests
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("listProjects() returns projects")
    void listProjects() {
        List<Project> projects = client.listProjects();

        assertNotNull(projects, "Projects list should not be null");
        assertFalse(projects.isEmpty(), "Should have at least one project");

        // Verify project structure
        Project firstProject = projects.get(0);
        assertNotNull(firstProject.getId(), "Project should have an ID");
        assertNotNull(firstProject.getName(), "Project should have a name");
    }

    @Test
    @Order(2)
    @DisplayName("getProject() returns project with environments")
    void getProject() {
        Project project = client.getProject(projectSlug);

        assertNotNull(project, "Project should not be null");
        assertNotNull(project.getId(), "Project should have an ID");
        assertNotNull(project.getName(), "Project should have a name");
        assertNotNull(project.getEnvironments(), "Project should have environments");
        assertFalse(project.getEnvironments().isEmpty(), "Project should have at least one environment");
    }

    // ============================================================
    // Environment Tests
    // ============================================================

    @Test
    @Order(3)
    @DisplayName("listEnvironments() returns environments")
    void listEnvironments() {
        List<Environment> environments = client.listEnvironments(projectSlug);

        assertNotNull(environments, "Environments list should not be null");
        assertFalse(environments.isEmpty(), "Should have at least one environment");

        // Check for expected environments
        List<String> envNames = environments.stream()
            .map(Environment::getName)
            .toList();

        assertTrue(
            envNames.contains("development") || envNames.contains("staging") || envNames.contains("production"),
            "Should have at least one standard environment (development, staging, or production)"
        );
    }

    // ============================================================
    // Secret CRUD Tests
    // ============================================================

    @Test
    @Order(10)
    @DisplayName("setSecret() creates a new secret")
    void createSecret() {
        assertDoesNotThrow(() -> {
            client.setSecret(projectSlug, environment, testSecretKey, testSecretValue, "Integration test secret");
        }, "Should create secret without throwing");

        secretCreated = true;

        // Verify secret was created by retrieving it
        SecretWithValue secret = client.getSecret(projectSlug, environment, testSecretKey);
        assertNotNull(secret, "Created secret should be retrievable");
        assertEquals(testSecretKey, secret.getKey(), "Secret key should match");
        assertEquals(testSecretValue, secret.getValue(), "Secret value should match");
    }

    @Test
    @Order(11)
    @DisplayName("getSecret() retrieves the created secret with value")
    void getSecret() {
        assumeTrue(secretCreated, "Secret must be created first");

        SecretWithValue secret = client.getSecret(projectSlug, environment, testSecretKey);

        assertNotNull(secret, "Secret should not be null");
        assertEquals(testSecretKey, secret.getKey(), "Secret key should match");
        assertEquals(testSecretValue, secret.getValue(), "Secret value should match");
    }

    @Test
    @Order(12)
    @DisplayName("getSecrets() returns all secrets including test secret")
    void getSecrets() {
        assumeTrue(secretCreated, "Secret must be created first");

        List<SecretWithValueAndInheritance> secrets = client.getSecrets(projectSlug, environment);

        assertNotNull(secrets, "Secrets list should not be null");

        // Find our test secret
        boolean found = secrets.stream()
            .anyMatch(s -> testSecretKey.equals(s.getKey()));
        assertTrue(found, "Should contain our test secret");
    }

    @Test
    @Order(13)
    @DisplayName("getSecretsAsMap() returns secrets as key-value map")
    void getSecretsAsMap() {
        assumeTrue(secretCreated, "Secret must be created first");

        // Clear cache to ensure fresh data
        client.clearCache(projectSlug, environment);

        Map<String, String> secrets = client.getSecretsAsMap(projectSlug, environment);

        assertNotNull(secrets, "Secrets map should not be null");
        assertTrue(secrets.containsKey(testSecretKey), "Map should contain our test secret key");
        assertEquals(testSecretValue, secrets.get(testSecretKey), "Secret value should match");
    }

    @Test
    @Order(14)
    @DisplayName("setSecret() updates existing secret")
    void updateSecret() {
        assumeTrue(secretCreated, "Secret must be created first");

        String updatedValue = "updated-value-" + System.currentTimeMillis();

        assertDoesNotThrow(() -> {
            client.setSecret(projectSlug, environment, testSecretKey, updatedValue);
        }, "Should update secret without throwing");

        // Clear cache and verify update
        client.clearCache(projectSlug, environment);
        SecretWithValue retrieved = client.getSecret(projectSlug, environment, testSecretKey);

        assertNotNull(retrieved, "Updated secret should be retrievable");
        assertEquals(updatedValue, retrieved.getValue(), "Secret value should be updated");

        // Update the expected value for subsequent tests
        testSecretValue = updatedValue;
    }

    @Test
    @Order(15)
    @DisplayName("generateEnvFile() generates valid .env content")
    void generateEnvFile() {
        assumeTrue(secretCreated, "Secret must be created first");

        String envContent = client.generateEnvFile(projectSlug, environment);

        assertNotNull(envContent, "Env file content should not be null");
        assertFalse(envContent.isEmpty(), "Env file content should not be empty");
        assertTrue(envContent.contains(testSecretKey + "="), "Env file should contain our test secret");
    }

    @Test
    @Order(20)
    @DisplayName("deleteSecret() removes the secret")
    void deleteSecret() {
        assumeTrue(secretCreated, "Secret must be created first");

        assertDoesNotThrow(() -> {
            client.deleteSecret(projectSlug, environment, testSecretKey);
        }, "Should delete secret without throwing");

        // Verify deletion by attempting to retrieve it
        client.clearCache(projectSlug, environment);
        KeyEnvException exception = assertThrows(KeyEnvException.class, () ->
            client.getSecret(projectSlug, environment, testSecretKey),
            "Getting deleted secret should throw exception"
        );

        assertTrue(exception.isNotFound(), "Exception should indicate not found");

        // Mark as not created so cleanup doesn't try again
        secretCreated = false;
    }

    // ============================================================
    // Error Handling Tests
    // ============================================================

    @Test
    @Order(30)
    @DisplayName("getSecret() throws KeyEnvException for non-existent secret")
    void getNonExistentSecret() {
        String nonExistentKey = "NON_EXISTENT_SECRET_" + System.currentTimeMillis();

        KeyEnvException exception = assertThrows(KeyEnvException.class, () ->
            client.getSecret(projectSlug, environment, nonExistentKey),
            "Getting non-existent secret should throw exception"
        );

        assertTrue(exception.isNotFound(), "Exception should indicate not found");
        assertEquals(404, exception.getStatus(), "Status should be 404");
    }

    @Test
    @Order(31)
    @DisplayName("getProject() throws KeyEnvException for non-existent project")
    void getNonExistentProject() {
        String nonExistentProject = "non-existent-project-" + System.currentTimeMillis();

        KeyEnvException exception = assertThrows(KeyEnvException.class, () ->
            client.getProject(nonExistentProject),
            "Getting non-existent project should throw exception"
        );

        assertTrue(exception.isNotFound() || exception.isForbidden(),
            "Exception should indicate not found or forbidden");
    }
}
