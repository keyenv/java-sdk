package dev.keyenv.types;

import java.util.List;

/**
 * Response containing the current user's permissions for a project.
 */
public class MyPermissionsResponse {

    private List<Permission> permissions;
    private boolean isTeamAdmin;

    public MyPermissionsResponse() {}

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    public boolean isTeamAdmin() {
        return isTeamAdmin;
    }

    public void setTeamAdmin(boolean teamAdmin) {
        isTeamAdmin = teamAdmin;
    }

    @Override
    public String toString() {
        return "MyPermissionsResponse{" +
            "permissions=" + permissions +
            ", isTeamAdmin=" + isTeamAdmin +
            '}';
    }
}
