package dev.keyenv;

import dev.keyenv.types.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
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
    private static List<String> extraCleanupKeys = new ArrayList<>();

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

        // Clean up extra keys created by bulk/multi-env/special-char tests
        if (client != null) {
            for (String key : extraCleanupKeys) {
                try {
                    client.deleteSecret(projectSlug, environment, key);
                    System.out.println("Cleaned up extra key: " + key);
                } catch (Exception e) {
                    System.out.println("Note: Could not delete extra key " + key + ": " + e.getMessage());
                }
            }
        }
    }

    // ============================================================
    // Token Validation Tests
    // ============================================================

    @Test
    @Order(0)
    @DisplayName("validateToken() returns token info without throwing")
    void validateToken() {
        CurrentUserResponse response = client.validateToken();

        assertNotNull(response, "Validate token response should not be null");
    }

    @Test
    @Order(0)
    @DisplayName("Invalid token throws unauthorized exception")
    void invalidToken_throws_unauthorized() {
        String baseUrl = System.getenv("KEYENV_API_URL");
        if (baseUrl.endsWith("/api/v1")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 7);
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        KeyEnv invalidClient = KeyEnv.builder()
            .token("invalid_token_xxx")
            .baseUrl(baseUrl)
            .build();

        KeyEnvException exception = assertThrows(KeyEnvException.class, () ->
            invalidClient.listProjects(),
            "Invalid token should throw exception"
        );

        assertTrue(exception.isUnauthorized(), "Exception should indicate unauthorized");
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
            .collect(java.util.stream.Collectors.toList());

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
    @DisplayName("exportSecrets() returns all secrets including test secret")
    void exportSecrets() {
        assumeTrue(secretCreated, "Secret must be created first");

        List<SecretWithValueAndInheritance> secrets = client.exportSecrets(projectSlug, environment);

        assertNotNull(secrets, "Secrets list should not be null");

        // Find our test secret
        boolean found = secrets.stream()
            .anyMatch(s -> testSecretKey.equals(s.getKey()));
        assertTrue(found, "Should contain our test secret");
    }

    @Test
    @Order(13)
    @DisplayName("exportSecretsAsMap() returns secrets as key-value map")
    void exportSecretsAsMap() {
        assumeTrue(secretCreated, "Secret must be created first");

        // Clear cache to ensure fresh data
        client.clearCache(projectSlug, environment);

        Map<String, String> secrets = client.exportSecretsAsMap(projectSlug, environment);

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
    @Order(16)
    @DisplayName("getSecretHistory() returns history after create and update")
    void getSecretHistory() {
        assumeTrue(secretCreated, "Secret must be created first");

        List<SecretHistory> history = client.getSecretHistory(projectSlug, environment, testSecretKey);

        assertNotNull(history, "History should not be null");
        assertFalse(history.isEmpty(), "History should have at least one entry");
        assertTrue(history.get(0).getVersion() >= 1, "History entry should have a version");
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

    // ============================================================
    // Bulk Import Tests
    // ============================================================

    private static String bulkKey1;
    private static String bulkKey2;
    private static String bulkKey3;

    @Test
    @Order(40)
    @DisplayName("bulkImport() creates new secrets")
    void bulkImport_creates() {
        String ts = String.valueOf(System.currentTimeMillis());
        bulkKey1 = "JAVA_BULK_A_" + ts;
        bulkKey2 = "JAVA_BULK_B_" + ts;
        bulkKey3 = "JAVA_BULK_C_" + ts;

        extraCleanupKeys.add(bulkKey1);
        extraCleanupKeys.add(bulkKey2);
        extraCleanupKeys.add(bulkKey3);

        List<SecretInput> secrets = List.of(
            new SecretInput(bulkKey1, "bulk-val-1"),
            new SecretInput(bulkKey2, "bulk-val-2"),
            new SecretInput(bulkKey3, "bulk-val-3")
        );

        BulkImportResult result = client.bulkImport(projectSlug, environment, secrets, BulkImportOptions.skipExisting());

        assertNotNull(result, "Bulk import result should not be null");
        assertEquals(3, result.getCreated(), "Should have created 3 secrets");
    }

    @Test
    @Order(41)
    @DisplayName("bulkImport() skips existing secrets when overwrite is false")
    void bulkImport_skips() {
        assumeTrue(bulkKey1 != null, "Bulk keys must be created first");

        List<SecretInput> secrets = List.of(
            new SecretInput(bulkKey1, "new-val-1")
        );

        BulkImportResult result = client.bulkImport(projectSlug, environment, secrets, BulkImportOptions.skipExisting());

        assertNotNull(result, "Bulk import result should not be null");
        assertEquals(1, result.getSkipped(), "Should have skipped 1 secret");
    }

    @Test
    @Order(42)
    @DisplayName("bulkImport() overwrites existing secrets when overwrite is true")
    void bulkImport_overwrites() {
        assumeTrue(bulkKey1 != null, "Bulk keys must be created first");

        List<SecretInput> secrets = List.of(
            new SecretInput(bulkKey1, "overwritten-val")
        );

        BulkImportResult result = client.bulkImport(projectSlug, environment, secrets, BulkImportOptions.withOverwrite());

        assertNotNull(result, "Bulk import result should not be null");
        assertEquals(1, result.getUpdated(), "Should have updated 1 secret");
    }

    // ============================================================
    // Load Env Test
    // ============================================================

    @Test
    @Order(50)
    @DisplayName("loadEnv() loads secrets into system properties")
    void loadEnv() {
        String loadEnvKey = "JAVA_LOADENV_" + System.currentTimeMillis();
        String loadEnvValue = "loadenv-value-" + System.currentTimeMillis();
        extraCleanupKeys.add(loadEnvKey);

        client.setSecret(projectSlug, environment, loadEnvKey, loadEnvValue);
        client.clearCache(projectSlug, environment);

        int count = client.loadEnv(projectSlug, environment);

        assertTrue(count > 0, "loadEnv should return a positive count");
        assertEquals(loadEnvValue, System.getProperty(loadEnvKey), "System property should contain the secret value");
    }

    // ============================================================
    // Multi-Environment Test
    // ============================================================

    @Test
    @Order(55)
    @DisplayName("Same key in different environments returns different values")
    void multiEnvironments() {
        String multiKey = "JAVA_MULTI_ENV_" + System.currentTimeMillis();
        String devValue = "dev-value-" + System.currentTimeMillis();
        String stagingValue = "staging-value-" + System.currentTimeMillis();

        extraCleanupKeys.add(multiKey);

        // Create in development (default environment)
        client.setSecret(projectSlug, environment, multiKey, devValue);

        // Create in staging
        client.setSecret(projectSlug, "staging", multiKey, stagingValue);

        // Verify each environment returns different values
        client.clearCache(projectSlug, environment);
        client.clearCache(projectSlug, "staging");

        SecretWithValue devSecret = client.getSecret(projectSlug, environment, multiKey);
        SecretWithValue stagingSecret = client.getSecret(projectSlug, "staging", multiKey);

        assertEquals(devValue, devSecret.getValue(), "Development value should match");
        assertEquals(stagingValue, stagingSecret.getValue(), "Staging value should match");
        assertNotEquals(devSecret.getValue(), stagingSecret.getValue(), "Values should differ across environments");

        // Cleanup staging
        try {
            client.deleteSecret(projectSlug, "staging", multiKey);
        } catch (Exception e) {
            System.out.println("Note: Could not delete staging key " + multiKey + ": " + e.getMessage());
        }
    }

    // ============================================================
    // Special Characters Test
    // ============================================================

    @Test
    @Order(56)
    @DisplayName("Secrets with special characters round-trip correctly")
    void specialCharacters() {
        String ts = String.valueOf(System.currentTimeMillis());

        // a) Connection string with special chars
        String connKey = "JAVA_CONN_STR_" + ts;
        String connValue = "postgresql://user:p@ss@localhost:5432/db?sslmode=require";
        extraCleanupKeys.add(connKey);

        client.setSecret(projectSlug, environment, connKey, connValue);
        client.clearCache(projectSlug, environment);
        SecretWithValue connSecret = client.getSecret(projectSlug, environment, connKey);
        assertEquals(connValue, connSecret.getValue(), "Connection string should round-trip correctly");

        // b) Multiline value
        String multilineKey = "JAVA_MULTILINE_" + ts;
        String multilineValue = "line1\nline2\nline3";
        extraCleanupKeys.add(multilineKey);

        client.setSecret(projectSlug, environment, multilineKey, multilineValue);
        client.clearCache(projectSlug, environment);
        SecretWithValue multilineSecret = client.getSecret(projectSlug, environment, multilineKey);
        assertEquals(multilineValue, multilineSecret.getValue(), "Multiline value should round-trip correctly");

        // c) JSON string value
        String jsonKey = "JAVA_JSON_" + ts;
        String jsonValue = "{\"host\":\"localhost\",\"port\":5432,\"ssl\":true}";
        extraCleanupKeys.add(jsonKey);

        client.setSecret(projectSlug, environment, jsonKey, jsonValue);
        client.clearCache(projectSlug, environment);
        SecretWithValue jsonSecret = client.getSecret(projectSlug, environment, jsonKey);
        assertEquals(jsonValue, jsonSecret.getValue(), "JSON string should round-trip correctly");
    }
}
