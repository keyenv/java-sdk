package dev.keyenv;

import dev.keyenv.types.BulkImportOptions;
import dev.keyenv.types.BulkImportResult;
import dev.keyenv.types.CurrentUserResponse;
import dev.keyenv.types.DefaultPermission;
import dev.keyenv.types.Environment;
import dev.keyenv.types.MyPermissionsResponse;
import dev.keyenv.types.Permission;
import dev.keyenv.types.PermissionInput;
import dev.keyenv.types.Project;
import dev.keyenv.types.SecretHistory;
import dev.keyenv.types.SecretInput;
import dev.keyenv.types.SecretWithInheritance;
import dev.keyenv.types.SecretWithValue;
import dev.keyenv.types.SecretWithValueAndInheritance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KeyEnv API client for secrets management.
 *
 * <p>Example usage:
 * <pre>{@code
 * KeyEnv client = KeyEnv.create(System.getenv("KEYENV_TOKEN"));
 *
 * // Load secrets into environment
 * int count = client.loadEnv("your-project-id", "production");
 * System.out.println("Loaded " + count + " secrets");
 *
 * // Get secrets as a map
 * Map<String, String> secrets = client.getSecretsAsMap("project-id", "production");
 * System.out.println(secrets.get("DATABASE_URL"));
 * }</pre>
 */
public class KeyEnv {

    /** Default API base URL. */
    public static final String DEFAULT_BASE_URL = "https://api.keyenv.dev";

    /** Default request timeout in seconds. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /** SDK version. */
    public static final String VERSION = "1.0.0";

    /** API version prefix. */
    private static final String API_PREFIX = "/api/v1";

    private final String baseUrl;
    private final String token;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private final Duration cacheTtl;
    private final Map<String, CacheEntry> cache;

    /**
     * Cache entry with expiration.
     */
    private static class CacheEntry {
        final Object data;
        final Instant expiresAt;

        CacheEntry(Object data, Instant expiresAt) {
            this.data = data;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Private constructor - use {@link #create(String)} or {@link Builder}.
     */
    private KeyEnv(Builder builder) {
        this.baseUrl = builder.baseUrl.endsWith("/")
            ? builder.baseUrl.substring(0, builder.baseUrl.length() - 1)
            : builder.baseUrl;
        this.token = builder.token;
        this.timeout = builder.timeout;
        this.cacheTtl = builder.cacheTtl;
        this.cache = new ConcurrentHashMap<>();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(builder.timeout)
            .build();

        // Configure Jackson ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Creates a new KeyEnv client with the given token.
     *
     * @param token the service token for authentication
     * @return a new KeyEnv client
     * @throws KeyEnvException if the token is null or empty
     */
    public static KeyEnv create(String token) {
        return builder().token(token).build();
    }

    /**
     * Creates a new builder for the KeyEnv client.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating a KeyEnv client with custom configuration.
     */
    public static class Builder {
        private String token;
        private String baseUrl = DEFAULT_BASE_URL;
        private Duration timeout = DEFAULT_TIMEOUT;
        private Duration cacheTtl = Duration.ZERO;

        private Builder() {}

        /**
         * Sets the authentication token (required).
         *
         * @param token the service token
         * @return this builder
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Sets the API base URL (optional).
         *
         * @param baseUrl the base URL
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the request timeout (optional, default 30s).
         *
         * @param timeout the timeout duration
         * @return this builder
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets the cache TTL (optional, zero disables caching).
         *
         * @param cacheTtl the cache TTL duration
         * @return this builder
         */
        public Builder cacheTtl(Duration cacheTtl) {
            this.cacheTtl = cacheTtl;
            return this;
        }

        /**
         * Builds the KeyEnv client.
         *
         * @return a new KeyEnv client
         * @throws KeyEnvException if the token is null or empty
         */
        public KeyEnv build() {
            if (token == null || token.isEmpty()) {
                throw new KeyEnvException("Token is required");
            }
            return new KeyEnv(this);
        }
    }

    // ============================================================
    // HTTP Request Methods
    // ============================================================

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + API_PREFIX + path))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", "keyenv-java/" + VERSION)
            .timeout(timeout);
    }

    private String get(String path) throws KeyEnvException {
        try {
            HttpRequest request = requestBuilder(path)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleResponse(response);
            return response.body();
        } catch (KeyEnvException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KeyEnvException("Request interrupted", e);
        } catch (IOException e) {
            throw new KeyEnvException("Network error: " + e.getMessage(), e);
        }
    }

    private CompletableFuture<String> getAsync(String path) {
        HttpRequest request = requestBuilder(path)
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                handleResponse(response);
                return response.body();
            });
    }

    private String post(String path, String body) throws KeyEnvException {
        try {
            HttpRequest request = requestBuilder(path)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleResponse(response);
            return response.body();
        } catch (KeyEnvException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KeyEnvException("Request interrupted", e);
        } catch (IOException e) {
            throw new KeyEnvException("Network error: " + e.getMessage(), e);
        }
    }

    private CompletableFuture<String> postAsync(String path, String body) {
        HttpRequest request = requestBuilder(path)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                handleResponse(response);
                return response.body();
            });
    }

    private String put(String path, String body) throws KeyEnvException {
        try {
            HttpRequest request = requestBuilder(path)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleResponse(response);
            return response.body();
        } catch (KeyEnvException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KeyEnvException("Request interrupted", e);
        } catch (IOException e) {
            throw new KeyEnvException("Network error: " + e.getMessage(), e);
        }
    }

    private CompletableFuture<String> putAsync(String path, String body) {
        HttpRequest request = requestBuilder(path)
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                handleResponse(response);
                return response.body();
            });
    }

    private String delete(String path) throws KeyEnvException {
        try {
            HttpRequest request = requestBuilder(path)
                .DELETE()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleResponse(response);
            return response.body();
        } catch (KeyEnvException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KeyEnvException("Request interrupted", e);
        } catch (IOException e) {
            throw new KeyEnvException("Network error: " + e.getMessage(), e);
        }
    }

    private CompletableFuture<String> deleteAsync(String path) {
        HttpRequest request = requestBuilder(path)
            .DELETE()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                handleResponse(response);
                return response.body();
            });
    }

    private void handleResponse(HttpResponse<String> response) throws KeyEnvException {
        int status = response.statusCode();
        if (status >= 400) {
            String message = "Unknown error";
            String code = null;

            try {
                JsonNode errorNode = objectMapper.readTree(response.body());
                if (errorNode.has("error")) {
                    message = errorNode.get("error").asText();
                }
                if (errorNode.has("code")) {
                    code = errorNode.get("code").asText();
                }
            } catch (JsonProcessingException e) {
                // Use status text as fallback
                message = response.body() != null && !response.body().isEmpty()
                    ? response.body()
                    : "HTTP " + status;
            }

            throw new KeyEnvException(status, message, code);
        }
    }

    private <T> T parseJson(String json, Class<T> clazz) throws KeyEnvException {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new KeyEnvException("Failed to parse response: " + e.getMessage(), e);
        }
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) throws KeyEnvException {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new KeyEnvException("Failed to parse response: " + e.getMessage(), e);
        }
    }

    private String toJson(Object obj) throws KeyEnvException {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new KeyEnvException("Failed to serialize request: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // Cache Methods
    // ============================================================

    @SuppressWarnings("unchecked")
    private <T> T getCached(String key) {
        if (cacheTtl.isZero()) {
            return null;
        }
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            if (!entry.isExpired()) {
                return (T) entry.data;
            }
            // Remove expired entry to prevent memory leaks
            cache.remove(key);
        }
        return null;
    }

    private void setCache(String key, Object data) {
        if (!cacheTtl.isZero()) {
            // Prune expired entries to prevent memory leaks
            cache.entrySet().removeIf(e -> e.getValue().isExpired());
            cache.put(key, new CacheEntry(data, Instant.now().plus(cacheTtl)));
        }
    }

    /**
     * Clears the cache for a specific project and environment.
     *
     * @param projectId the project ID
     * @param environment the environment name
     */
    public void clearCache(String projectId, String environment) {
        String prefix = "secrets:" + projectId + ":" + environment;
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    /**
     * Clears all cached data.
     */
    public void clearAllCache() {
        cache.clear();
    }

    // ============================================================
    // User & Token Methods
    // ============================================================

    /**
     * Gets information about the current authenticated user or service token.
     *
     * @return the current user response
     * @throws KeyEnvException if the request fails
     */
    public CurrentUserResponse getCurrentUser() throws KeyEnvException {
        String response = get("/users/me");
        JsonNode root = parseJson(response, JsonNode.class);
        JsonNode data = root.has("data") ? root.get("data") : root;
        return parseJson(data.toString(), CurrentUserResponse.class);
    }

    /**
     * Async version of {@link #getCurrentUser()}.
     */
    public CompletableFuture<CurrentUserResponse> getCurrentUserAsync() {
        return getAsync("/users/me")
            .thenApply(response -> {
                JsonNode root = parseJson(response, JsonNode.class);
                JsonNode data = root.has("data") ? root.get("data") : root;
                return parseJson(data.toString(), CurrentUserResponse.class);
            });
    }

    /**
     * Validates the token and returns user info.
     *
     * @return the current user response
     * @throws KeyEnvException if the token is invalid
     */
    public CurrentUserResponse validateToken() throws KeyEnvException {
        return getCurrentUser();
    }

    // ============================================================
    // Project Methods
    // ============================================================

    /**
     * Lists all accessible projects.
     *
     * @return list of projects
     * @throws KeyEnvException if the request fails
     */
    public List<Project> listProjects() throws KeyEnvException {
        String response = get("/projects");
        JsonNode root = parseJson(response, JsonNode.class);
        return parseJson(root.get("projects").toString(), new TypeReference<List<Project>>() {});
    }

    /**
     * Async version of {@link #listProjects()}.
     */
    public CompletableFuture<List<Project>> listProjectsAsync() {
        return getAsync("/projects")
            .thenApply(response -> {
                JsonNode root = parseJson(response, JsonNode.class);
                return parseJson(root.get("projects").toString(), new TypeReference<List<Project>>() {});
            });
    }

    /**
     * Gets a project by ID.
     *
     * @param projectId the project ID
     * @return the project with its environments
     * @throws KeyEnvException if the request fails
     */
    public Project getProject(String projectId) throws KeyEnvException {
        String response = get("/projects/" + projectId);
        JsonNode root = parseJson(response, JsonNode.class);
        JsonNode data = root.has("data") ? root.get("data") : root;
        return parseJson(data.toString(), Project.class);
    }

    /**
     * Async version of {@link #getProject(String)}.
     */
    public CompletableFuture<Project> getProjectAsync(String projectId) {
        return getAsync("/projects/" + projectId)
            .thenApply(response -> {
                JsonNode root = parseJson(response, JsonNode.class);
                JsonNode data = root.has("data") ? root.get("data") : root;
                return parseJson(data.toString(), Project.class);
            });
    }

    /**
     * Creates a new project.
     *
     * @param teamId the team ID
     * @param name the project name
     * @return the created project
     * @throws KeyEnvException if the request fails
     */
    public Project createProject(String teamId, String name) throws KeyEnvException {
        Map<String, String> body = new HashMap<>();
        body.put("team_id", teamId);
        body.put("name", name);
        String response = post("/projects", toJson(body));
        JsonNode root = parseJson(response, JsonNode.class);
        JsonNode data = root.has("data") ? root.get("data") : root;
        return parseJson(data.toString(), Project.class);
    }

    /**
     * Deletes a project.
     *
     * @param projectId the project ID
     * @throws KeyEnvException if the request fails
     */
    public void deleteProject(String projectId) throws KeyEnvException {
        delete("/projects/" + projectId);
    }

    // ============================================================
    // Environment Methods
    // ============================================================

    /**
     * Lists environments in a project.
     *
     * @param projectId the project ID
     * @return list of environments
     * @throws KeyEnvException if the request fails
     */
    public List<Environment> listEnvironments(String projectId) throws KeyEnvException {
        String response = get("/projects/" + projectId + "/environments");
        JsonNode root = parseJson(response, JsonNode.class);
        return parseJson(root.get("environments").toString(), new TypeReference<List<Environment>>() {});
    }

    /**
     * Async version of {@link #listEnvironments(String)}.
     */
    public CompletableFuture<List<Environment>> listEnvironmentsAsync(String projectId) {
        return getAsync("/projects/" + projectId + "/environments")
            .thenApply(response -> {
                JsonNode root = parseJson(response, JsonNode.class);
                return parseJson(root.get("environments").toString(), new TypeReference<List<Environment>>() {});
            });
    }

    /**
     * Creates a new environment in a project.
     *
     * @param projectId the project ID
     * @param name the environment name
     * @param inheritsFrom optional environment ID to inherit from
     * @return the created environment
     * @throws KeyEnvException if the request fails
     */
    public Environment createEnvironment(String projectId, String name, String inheritsFrom) throws KeyEnvException {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        if (inheritsFrom != null) {
            body.put("inherits_from", inheritsFrom);
        }
        String response = post("/projects/" + projectId + "/environments", toJson(body));
        JsonNode root = parseJson(response, JsonNode.class);
        JsonNode data = root.has("data") ? root.get("data") : root;
        return parseJson(data.toString(), Environment.class);
    }

    /**
     * Deletes an environment from a project.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @throws KeyEnvException if the request fails
     */
    public void deleteEnvironment(String projectId, String environment) throws KeyEnvException {
        delete("/projects/" + projectId + "/environments/" + environment);
    }

    // ============================================================
    // Secret Methods
    // ============================================================

    /**
     * Lists secrets (without values) in an environment.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @return list of secrets without values
     * @throws KeyEnvException if the request fails
     */
    public List<SecretWithInheritance> listSecrets(String projectId, String environment) throws KeyEnvException {
        String response = get("/projects/" + projectId + "/environments/" + environment + "/secrets");
        JsonNode root = parseJson(response, JsonNode.class);
        return parseJson(root.get("secrets").toString(), new TypeReference<List<SecretWithInheritance>>() {});
    }

    /**
     * Async version of {@link #listSecrets(String, String)}.
     */
    public CompletableFuture<List<SecretWithInheritance>> listSecretsAsync(String projectId, String environment) {
        return getAsync("/projects/" + projectId + "/environments/" + environment + "/secrets")
            .thenApply(response -> {
                JsonNode root = parseJson(response, JsonNode.class);
                return parseJson(root.get("secrets").toString(), new TypeReference<List<SecretWithInheritance>>() {});
            });
    }

    /**
     * Exports all secrets with their values from an environment.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @return list of secrets with values
     * @throws KeyEnvException if the request fails
     */
    public List<SecretWithValueAndInheritance> getSecrets(String projectId, String environment) throws KeyEnvException {
        // Check cache first
        String cacheKey = "secrets:" + projectId + ":" + environment + ":export";
        List<SecretWithValueAndInheritance> cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }

        String response = get("/projects/" + projectId + "/environments/" + environment + "/secrets/export");
        JsonNode root = parseJson(response, JsonNode.class);
        List<SecretWithValueAndInheritance> secrets = parseJson(
            root.get("secrets").toString(),
            new TypeReference<List<SecretWithValueAndInheritance>>() {}
        );

        setCache(cacheKey, secrets);
        return secrets;
    }

    /**
     * Async version of {@link #getSecrets(String, String)}.
     */
    public CompletableFuture<List<SecretWithValueAndInheritance>> getSecretsAsync(String projectId, String environment) {
        // Check cache first
        String cacheKey = "secrets:" + projectId + ":" + environment + ":export";
        List<SecretWithValueAndInheritance> cached = getCached(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return getAsync("/projects/" + projectId + "/environments/" + environment + "/secrets/export")
            .thenApply(response -> {
                JsonNode root = parseJson(response, JsonNode.class);
                List<SecretWithValueAndInheritance> secrets = parseJson(
                    root.get("secrets").toString(),
                    new TypeReference<List<SecretWithValueAndInheritance>>() {}
                );
                setCache(cacheKey, secrets);
                return secrets;
            });
    }

    /**
     * Gets secrets as a key-value map.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @return map of secret keys to values
     * @throws KeyEnvException if the request fails
     */
    public Map<String, String> getSecretsAsMap(String projectId, String environment) throws KeyEnvException {
        List<SecretWithValueAndInheritance> secrets = getSecrets(projectId, environment);
        Map<String, String> result = new HashMap<>();
        for (SecretWithValueAndInheritance secret : secrets) {
            result.put(secret.getKey(), secret.getValue());
        }
        return result;
    }

    /**
     * Async version of {@link #getSecretsAsMap(String, String)}.
     */
    public CompletableFuture<Map<String, String>> getSecretsAsMapAsync(String projectId, String environment) {
        return getSecretsAsync(projectId, environment).thenApply(secrets -> {
            Map<String, String> result = new HashMap<>();
            for (SecretWithValueAndInheritance secret : secrets) {
                result.put(secret.getKey(), secret.getValue());
            }
            return result;
        });
    }

    /**
     * Gets a single secret by key.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @param key the secret key
     * @return the secret with its value
     * @throws KeyEnvException if the request fails
     */
    public SecretWithValue getSecret(String projectId, String environment, String key) throws KeyEnvException {
        String response = get("/projects/" + projectId + "/environments/" + environment + "/secrets/" + key);
        JsonNode root = parseJson(response, JsonNode.class);
        return parseJson(root.get("secret").toString(), SecretWithValue.class);
    }

    /**
     * Async version of {@link #getSecret(String, String, String)}.
     */
    public CompletableFuture<SecretWithValue> getSecretAsync(String projectId, String environment, String key) {
        return getAsync("/projects/" + projectId + "/environments/" + environment + "/secrets/" + key)
            .thenApply(response -> {
                JsonNode root = parseJson(response, JsonNode.class);
                return parseJson(root.get("secret").toString(), SecretWithValue.class);
            });
    }

    /**
     * Sets (creates or updates) a secret.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @param key the secret key
     * @param value the secret value
     * @throws KeyEnvException if the request fails
     */
    public void setSecret(String projectId, String environment, String key, String value) throws KeyEnvException {
        setSecret(projectId, environment, key, value, null);
    }

    /**
     * Sets (creates or updates) a secret with an optional description.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @param key the secret key
     * @param value the secret value
     * @param description optional description
     * @throws KeyEnvException if the request fails
     */
    public void setSecret(String projectId, String environment, String key, String value, String description) throws KeyEnvException {
        String path = "/projects/" + projectId + "/environments/" + environment + "/secrets/" + key;
        Map<String, String> body = new HashMap<>();
        body.put("value", value);
        if (description != null) {
            body.put("description", description);
        }

        try {
            // Try to update first
            put(path, toJson(body));
        } catch (KeyEnvException e) {
            if (e.isNotFound()) {
                // Create if not found
                Map<String, String> createBody = new HashMap<>();
                createBody.put("key", key);
                createBody.put("value", value);
                if (description != null) {
                    createBody.put("description", description);
                }
                post("/projects/" + projectId + "/environments/" + environment + "/secrets", toJson(createBody));
            } else {
                throw e;
            }
        }

        clearCache(projectId, environment);
    }

    /**
     * Async version of {@link #setSecret(String, String, String, String, String)}.
     */
    public CompletableFuture<Void> setSecretAsync(String projectId, String environment, String key, String value, String description) {
        String path = "/projects/" + projectId + "/environments/" + environment + "/secrets/" + key;
        Map<String, String> body = new HashMap<>();
        body.put("value", value);
        if (description != null) {
            body.put("description", description);
        }

        return putAsync(path, toJson(body))
            .<Void>handle((response, ex) -> {
                if (ex == null) {
                    clearCache(projectId, environment);
                    return null;
                }
                if (ex.getCause() instanceof KeyEnvException && ((KeyEnvException) ex.getCause()).isNotFound()) {
                    // Create if not found
                    Map<String, String> createBody = new HashMap<>();
                    createBody.put("key", key);
                    createBody.put("value", value);
                    if (description != null) {
                        createBody.put("description", description);
                    }
                    post("/projects/" + projectId + "/environments/" + environment + "/secrets", toJson(createBody));
                    clearCache(projectId, environment);
                    return null;
                }
                if (ex.getCause() instanceof KeyEnvException) {
                    throw (KeyEnvException) ex.getCause();
                }
                throw new KeyEnvException(ex.getMessage(), ex);
            });
    }

    /**
     * Deletes a secret.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @param key the secret key
     * @throws KeyEnvException if the request fails
     */
    public void deleteSecret(String projectId, String environment, String key) throws KeyEnvException {
        delete("/projects/" + projectId + "/environments/" + environment + "/secrets/" + key);
        clearCache(projectId, environment);
    }

    /**
     * Async version of {@link #deleteSecret(String, String, String)}.
     */
    public CompletableFuture<Void> deleteSecretAsync(String projectId, String environment, String key) {
        return deleteAsync("/projects/" + projectId + "/environments/" + environment + "/secrets/" + key)
            .thenApply(response -> {
                clearCache(projectId, environment);
                return null;
            });
    }

    /**
     * Bulk imports secrets.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @param secrets list of secrets to import
     * @param options import options
     * @return the import result
     * @throws KeyEnvException if the request fails
     */
    public BulkImportResult bulkImport(String projectId, String environment, List<SecretInput> secrets, BulkImportOptions options) throws KeyEnvException {
        Map<String, Object> body = new HashMap<>();
        body.put("secrets", secrets);
        body.put("overwrite", options != null && options.isOverwrite());

        String response = post("/projects/" + projectId + "/environments/" + environment + "/secrets/bulk", toJson(body));
        clearCache(projectId, environment);
        return parseJson(response, BulkImportResult.class);
    }

    /**
     * Loads secrets into system environment variables.
     * Note: This only affects the current JVM process.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @return the number of secrets loaded
     * @throws KeyEnvException if the request fails
     */
    public int loadEnv(String projectId, String environment) throws KeyEnvException {
        List<SecretWithValueAndInheritance> secrets = getSecrets(projectId, environment);
        for (SecretWithValueAndInheritance secret : secrets) {
            // Note: System.setProperty is used since Java doesn't allow modifying env vars
            System.setProperty(secret.getKey(), secret.getValue());
        }
        return secrets.size();
    }

    /**
     * Async version of {@link #loadEnv(String, String)}.
     */
    public CompletableFuture<Integer> loadEnvAsync(String projectId, String environment) {
        return getSecretsAsync(projectId, environment).thenApply(secrets -> {
            for (SecretWithValueAndInheritance secret : secrets) {
                System.setProperty(secret.getKey(), secret.getValue());
            }
            return secrets.size();
        });
    }

    /**
     * Generates .env file content.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @return the .env file content
     * @throws KeyEnvException if the request fails
     */
    public String generateEnvFile(String projectId, String environment) throws KeyEnvException {
        List<SecretWithValueAndInheritance> secrets = getSecrets(projectId, environment);
        StringBuilder builder = new StringBuilder();

        for (SecretWithValueAndInheritance secret : secrets) {
            String value = secret.getValue();
            boolean needsQuotes = value.contains(" ") || value.contains("\t") ||
                value.contains("\n") || value.contains("\"") ||
                value.contains("'") || value.contains("\\") || value.contains("$");

            if (needsQuotes) {
                String escaped = value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("$", "\\$");
                builder.append(secret.getKey()).append("=\"").append(escaped).append("\"\n");
            } else {
                builder.append(secret.getKey()).append("=").append(value).append("\n");
            }
        }

        return builder.toString();
    }

    // ============================================================
    // Permission Methods
    // ============================================================

    /**
     * Lists permissions for an environment.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @return list of permissions
     * @throws KeyEnvException if the request fails
     */
    public List<Permission> listPermissions(String projectId, String environment) throws KeyEnvException {
        String response = get("/projects/" + projectId + "/environments/" + environment + "/permissions");
        JsonNode root = parseJson(response, JsonNode.class);
        return parseJson(root.get("permissions").toString(), new TypeReference<List<Permission>>() {});
    }

    /**
     * Sets a user's permission for an environment.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @param userId the user ID
     * @param role the role (admin, write, read, none)
     * @throws KeyEnvException if the request fails
     */
    public void setPermission(String projectId, String environment, String userId, String role) throws KeyEnvException {
        Map<String, String> body = new HashMap<>();
        body.put("role", role);
        put("/projects/" + projectId + "/environments/" + environment + "/permissions/" + userId, toJson(body));
    }

    /**
     * Deletes a user's permission for an environment.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @param userId the user ID
     * @throws KeyEnvException if the request fails
     */
    public void deletePermission(String projectId, String environment, String userId) throws KeyEnvException {
        delete("/projects/" + projectId + "/environments/" + environment + "/permissions/" + userId);
    }

    /**
     * Bulk sets permissions.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @param permissions list of permissions to set
     * @throws KeyEnvException if the request fails
     */
    public void bulkSetPermissions(String projectId, String environment, List<PermissionInput> permissions) throws KeyEnvException {
        Map<String, Object> body = new HashMap<>();
        body.put("permissions", permissions);
        put("/projects/" + projectId + "/environments/" + environment + "/permissions", toJson(body));
    }

    /**
     * Gets the current user's permissions for a project.
     *
     * @param projectId the project ID
     * @return the permissions response
     * @throws KeyEnvException if the request fails
     */
    public MyPermissionsResponse getMyPermissions(String projectId) throws KeyEnvException {
        String response = get("/projects/" + projectId + "/my-permissions");
        return parseJson(response, MyPermissionsResponse.class);
    }

    /**
     * Gets default permissions for a project.
     *
     * @param projectId the project ID
     * @return list of default permissions
     * @throws KeyEnvException if the request fails
     */
    public List<DefaultPermission> getProjectDefaults(String projectId) throws KeyEnvException {
        String response = get("/projects/" + projectId + "/permissions/defaults");
        JsonNode root = parseJson(response, JsonNode.class);
        return parseJson(root.get("defaults").toString(), new TypeReference<List<DefaultPermission>>() {});
    }

    /**
     * Sets default permissions for a project.
     *
     * @param projectId the project ID
     * @param defaults list of default permissions
     * @throws KeyEnvException if the request fails
     */
    public void setProjectDefaults(String projectId, List<DefaultPermission> defaults) throws KeyEnvException {
        Map<String, Object> body = new HashMap<>();
        body.put("defaults", defaults);
        put("/projects/" + projectId + "/permissions/defaults", toJson(body));
    }

    // ============================================================
    // History Methods
    // ============================================================

    /**
     * Gets the version history of a secret.
     *
     * @param projectId the project ID
     * @param environment the environment name
     * @param key the secret key
     * @return list of history entries
     * @throws KeyEnvException if the request fails
     */
    public List<SecretHistory> getSecretHistory(String projectId, String environment, String key) throws KeyEnvException {
        String response = get("/projects/" + projectId + "/environments/" + environment + "/secrets/" + key + "/history");
        JsonNode root = parseJson(response, JsonNode.class);
        return parseJson(root.get("history").toString(), new TypeReference<List<SecretHistory>>() {});
    }

    /**
     * Async version of {@link #getSecretHistory(String, String, String)}.
     */
    public CompletableFuture<List<SecretHistory>> getSecretHistoryAsync(String projectId, String environment, String key) {
        return getAsync("/projects/" + projectId + "/environments/" + environment + "/secrets/" + key + "/history")
            .thenApply(response -> {
                JsonNode root = parseJson(response, JsonNode.class);
                return parseJson(root.get("history").toString(), new TypeReference<List<SecretHistory>>() {});
            });
    }
}
