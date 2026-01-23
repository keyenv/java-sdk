package dev.keyenv.types;

/**
 * Represents default permission settings for an environment.
 */
public class DefaultPermission {

    private String environmentName;
    private String defaultRole;

    public DefaultPermission() {}

    public DefaultPermission(String environmentName, String defaultRole) {
        this.environmentName = environmentName;
        this.defaultRole = defaultRole;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    @Override
    public String toString() {
        return "DefaultPermission{" +
            "environmentName='" + environmentName + '\'' +
            ", defaultRole='" + defaultRole + '\'' +
            '}';
    }
}
