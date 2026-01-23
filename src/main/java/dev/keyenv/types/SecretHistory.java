package dev.keyenv.types;

import java.time.Instant;

/**
 * Represents a historical version of a secret.
 */
public class SecretHistory {

    private String id;
    private String secretId;
    private String key;
    private int version;
    private String changedBy;
    private String changeType;
    private Instant createdAt;

    public SecretHistory() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SecretHistory{" +
            "id='" + id + '\'' +
            ", key='" + key + '\'' +
            ", version=" + version +
            ", changeType='" + changeType + '\'' +
            '}';
    }
}
