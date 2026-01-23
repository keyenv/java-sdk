package dev.keyenv.types;

import java.time.Instant;

/**
 * Represents a KeyEnv environment within a project.
 */
public class Environment {

    private String id;
    private String name;
    private String description;
    private String projectId;
    private String inheritsFromId;
    private int order;
    private Instant createdAt;
    private Instant updatedAt;

    public Environment() {}

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getInheritsFromId() {
        return inheritsFromId;
    }

    public void setInheritsFromId(String inheritsFromId) {
        this.inheritsFromId = inheritsFromId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Environment{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", projectId='" + projectId + '\'' +
            '}';
    }
}
