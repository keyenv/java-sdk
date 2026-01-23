package dev.keyenv.types;

import java.time.Instant;

/**
 * Represents a KeyEnv team.
 */
public class Team {

    private String id;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;

    public Team() {}

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
        return "Team{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            '}';
    }
}
