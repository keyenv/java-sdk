# KeyEnv Java SDK

Official Java SDK for [KeyEnv](https://keyenv.dev) - Secrets management made simple.

[![JitPack](https://jitpack.io/v/keyenv/java-sdk.svg)](https://jitpack.io/#keyenv/java-sdk)

## Installation

### Maven

Add the JitPack repository and dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.keyenv</groupId>
    <artifactId>java-sdk</artifactId>
    <version>v1.1.0</version>
</dependency>
```

### Gradle

Add the JitPack repository and dependency to your `build.gradle`:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.keyenv:java-sdk:v1.1.0'
}
```

## Quick Start

```java
import dev.keyenv.KeyEnv;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Create client
        KeyEnv client = KeyEnv.create(System.getenv("KEYENV_TOKEN"));

        // Load secrets into system properties
        int count = client.loadEnv("your-project-id", "production");
        System.out.println("Loaded " + count + " secrets");

        // Access secrets
        System.out.println(System.getProperty("DATABASE_URL"));
    }
}
```

## Configuration

```java
import dev.keyenv.KeyEnv;
import java.time.Duration;

KeyEnv client = KeyEnv.builder()
    .token("your-service-token")           // Required
    .baseUrl("https://api.keyenv.dev")     // Optional, default shown
    .timeout(Duration.ofSeconds(30))       // Optional, default 30s
    .cacheTtl(Duration.ofMinutes(5))       // Optional, 0 disables caching
    .build();
```

## Loading Secrets

### Load into System Properties

The simplest way to use secrets in your application:

```java
int count = client.loadEnv("project-id", "production");
System.out.println("Loaded " + count + " secrets");

// Access them via System.getProperty()
String dbUrl = System.getProperty("DATABASE_URL");
```

> **Note:** Java doesn't allow modifying environment variables at runtime, so secrets are loaded into system properties instead.

### Export as Map

Get secrets as a Map:

```java
Map<String, String> secrets = client.getSecretsAsMap("project-id", "production");
System.out.println(secrets.get("DATABASE_URL"));
```

### Export with Metadata

Get secrets with full metadata:

```java
List<SecretWithValueAndInheritance> secrets = client.getSecrets("project-id", "production");
for (var secret : secrets) {
    System.out.println(secret.getKey() + "=" + secret.getValue());
    if (secret.isInherited()) {
        System.out.println("  (inherited from " + secret.getInheritedFrom() + ")");
    }
}
```

## Managing Secrets

### Get a Single Secret

```java
SecretWithValue secret = client.getSecret("project-id", "production", "DATABASE_URL");
System.out.println(secret.getValue());
```

### Set a Secret

Creates or updates a secret:

```java
client.setSecret("project-id", "production", "API_KEY", "sk_live_...");

// With description
client.setSecret("project-id", "production", "API_KEY", "sk_live_...", "Production API key");
```

### Delete a Secret

```java
client.deleteSecret("project-id", "production", "OLD_KEY");
```

## Bulk Operations

### Bulk Import

```java
import dev.keyenv.types.SecretInput;
import dev.keyenv.types.BulkImportOptions;
import dev.keyenv.types.BulkImportResult;

List<SecretInput> secrets = List.of(
    new SecretInput("DATABASE_URL", "postgres://localhost/mydb"),
    new SecretInput("REDIS_URL", "redis://localhost:6379"),
    new SecretInput("API_KEY", "sk_test_123", "Test API key")  // with description
);

BulkImportResult result = client.bulkImport(
    "project-id",
    "development",
    secrets,
    BulkImportOptions.withOverwrite()  // or BulkImportOptions.skipExisting()
);

System.out.println("Created: " + result.getCreated() + ", Updated: " + result.getUpdated());
```

### Generate .env File

```java
String content = client.generateEnvFile("project-id", "production");
Files.writeString(Path.of(".env"), content);
```

## Projects & Environments

### List Projects

```java
List<Project> projects = client.listProjects();
for (Project project : projects) {
    System.out.println(project.getName() + " (" + project.getId() + ")");
}
```

### Get Project Details

```java
Project project = client.getProject("project-id");
System.out.println("Project: " + project.getName());
for (Environment env : project.getEnvironments()) {
    System.out.println("  - " + env.getName());
}
```

## Async Operations

All methods have async variants using `CompletableFuture`:

```java
client.getSecretsAsync("project-id", "production")
    .thenApply(secrets -> {
        System.out.println("Loaded " + secrets.size() + " secrets");
        return secrets;
    })
    .exceptionally(e -> {
        System.err.println("Error: " + e.getMessage());
        return List.of();
    });
```

## Error Handling

```java
import dev.keyenv.KeyEnvException;

try {
    client.getSecret("project-id", "production", "MISSING_KEY");
} catch (KeyEnvException e) {
    if (e.isNotFound()) {
        System.out.println("Secret not found");
    } else if (e.isUnauthorized()) {
        System.out.println("Invalid or expired token");
    } else if (e.isForbidden()) {
        System.out.println("Access denied");
    } else if (e.isRateLimited()) {
        System.out.println("Rate limited - slow down!");
    } else {
        System.out.println("Error " + e.getStatus() + ": " + e.getMessage());
    }
}
```

## Caching

Enable caching for better performance in serverless environments:

```java
KeyEnv client = KeyEnv.builder()
    .token(System.getenv("KEYENV_TOKEN"))
    .cacheTtl(Duration.ofMinutes(5))
    .build();

// Cached for 5 minutes
var secrets = client.getSecrets("project-id", "production");

// Clear cache manually
client.clearCache("project-id", "production");

// Or clear all cache
client.clearAllCache();
```

## API Reference

### Constructor Options

| Option | Type | Required | Default | Description |
|--------|------|----------|---------|-------------|
| `token` | `String` | Yes | - | Service token |
| `baseUrl` | `String` | No | `https://api.keyenv.dev` | API base URL |
| `timeout` | `Duration` | No | `30s` | Request timeout |
| `cacheTtl` | `Duration` | No | `0` | Cache TTL (0 = disabled) |

### Methods

| Method | Description |
|--------|-------------|
| `getCurrentUser()` | Get current user/token info |
| `validateToken()` | Validate token and get user info |
| `listProjects()` | List all accessible projects |
| `getProject(id)` | Get project with environments |
| `createProject(teamId, name)` | Create a new project |
| `deleteProject(id)` | Delete a project |
| `listEnvironments(projectId)` | List environments |
| `createEnvironment(projectId, name, inheritsFrom)` | Create environment |
| `deleteEnvironment(projectId, env)` | Delete environment |
| `listSecrets(projectId, env)` | List secret keys (no values) |
| `getSecrets(projectId, env)` | Export secrets with values |
| `getSecretsAsMap(projectId, env)` | Export as Map |
| `getSecret(projectId, env, key)` | Get single secret |
| `setSecret(projectId, env, key, value)` | Create or update secret |
| `setSecret(projectId, env, key, value, desc)` | Create/update with description |
| `deleteSecret(projectId, env, key)` | Delete secret |
| `bulkImport(projectId, env, secrets, opts)` | Bulk import secrets |
| `loadEnv(projectId, env)` | Load secrets into System.properties |
| `generateEnvFile(projectId, env)` | Generate .env file content |
| `getSecretHistory(projectId, env, key)` | Get secret version history |
| `listPermissions(projectId, env)` | List permissions |
| `setPermission(projectId, env, userId, role)` | Set user permission |
| `deletePermission(projectId, env, userId)` | Delete permission |
| `bulkSetPermissions(projectId, env, perms)` | Bulk set permissions |
| `getMyPermissions(projectId)` | Get current user's permissions |
| `getProjectDefaults(projectId)` | Get default permissions |
| `setProjectDefaults(projectId, defaults)` | Set default permissions |
| `clearCache(projectId, env)` | Clear cached secrets |
| `clearAllCache()` | Clear all cached data |

All methods also have async variants with `Async` suffix returning `CompletableFuture<T>`.

## Examples

### Spring Boot Application

```java
import dev.keyenv.KeyEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class Application {

    @PostConstruct
    public void loadSecrets() {
        KeyEnv client = KeyEnv.create(System.getenv("KEYENV_TOKEN"));
        client.loadEnv(System.getenv("KEYENV_PROJECT"), "production");
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### AWS Lambda Function

```java
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import dev.keyenv.KeyEnv;
import java.time.Duration;

public class Handler implements RequestHandler<Object, String> {

    // Reuse client across warm invocations
    private static final KeyEnv client;

    static {
        client = KeyEnv.builder()
            .token(System.getenv("KEYENV_TOKEN"))
            .cacheTtl(Duration.ofMinutes(5))  // Cache across warm invocations
            .build();

        // Load secrets once during cold start
        client.loadEnv(System.getenv("KEYENV_PROJECT"), "production");
    }

    @Override
    public String handleRequest(Object input, Context context) {
        return System.getProperty("API_KEY");
    }
}
```

## Requirements

- Java 11 or higher
- No additional runtime dependencies (uses built-in `java.net.http.HttpClient`)

## Building from Source

```bash
# Maven
mvn clean install

# Gradle
./gradlew build
```

## License

MIT License - see [LICENSE](LICENSE) for details.
