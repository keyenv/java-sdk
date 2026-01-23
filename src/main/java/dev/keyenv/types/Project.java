package dev.keyenv.types;

import java.time.Instant;
import java.util.List;

/**
 * Represents a KeyEnv project.
 */
public class Project {

    private String id;
    private String name;
    private String description;
    private String teamId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Environment> environments;

    public Project() {}

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

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
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

    public List<Environment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<Environment> environments) {
        this.environments = environments;
    }

    @Override
    public String toString() {
        return "Project{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", teamId='" + teamId + '\'' +
            '}';
    }
}
