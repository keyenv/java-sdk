package dev.keyenv.types;

/**
 * Input for setting a permission.
 */
public class PermissionInput {

    private String userId;
    private String role;

    public PermissionInput() {}

    public PermissionInput(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "PermissionInput{" +
            "userId='" + userId + '\'' +
            ", role='" + role + '\'' +
            '}';
    }
}
