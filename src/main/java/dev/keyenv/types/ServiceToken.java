package dev.keyenv.types;

import java.time.Instant;
import java.util.List;

/**
 * Represents information about a service token.
 */
public class ServiceToken {

    private String id;
    private String name;
    private String projectId;
    private String projectName;
    private List<String> permissions;
    private Instant expiresAt;
    private Instant createdAt;

    public ServiceToken() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns true if the token is expired.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "ServiceToken{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", projectId='" + projectId + '\'' +
            '}';
    }
}
