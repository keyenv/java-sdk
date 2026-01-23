package dev.keyenv.types;

/**
 * Response containing information about the current authenticated user or token.
 */
public class CurrentUserResponse {

    /** Authentication type: "user" or "service_token" */
    private String type;
    private User user;
    private ServiceToken serviceToken;

    public CurrentUserResponse() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ServiceToken getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(ServiceToken serviceToken) {
        this.serviceToken = serviceToken;
    }

    /**
     * Returns true if authenticated as a user.
     *
     * @return true if user authentication
     */
    public boolean isUser() {
        return "user".equals(type);
    }

    /**
     * Returns true if authenticated as a service token.
     *
     * @return true if service token authentication
     */
    public boolean isServiceToken() {
        return "service_token".equals(type);
    }

    @Override
    public String toString() {
        return "CurrentUserResponse{" +
            "type='" + type + '\'' +
            ", user=" + user +
            ", serviceToken=" + serviceToken +
            '}';
    }
}
