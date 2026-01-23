package dev.keyenv;

/**
 * Exception thrown when a KeyEnv API operation fails.
 *
 * <p>This exception provides access to the HTTP status code, error message,
 * and optional error code from the API response.
 *
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     client.getSecret("project-id", "production", "MISSING_KEY");
 * } catch (KeyEnvException e) {
 *     if (e.isNotFound()) {
 *         System.out.println("Secret not found");
 *     } else if (e.isUnauthorized()) {
 *         System.out.println("Invalid or expired token");
 *     } else {
 *         System.out.println("Error " + e.getStatus() + ": " + e.getMessage());
 *     }
 * }
 * }</pre>
 */
public class KeyEnvException extends RuntimeException {

    private final int status;
    private final String code;

    /**
     * Creates a new KeyEnvException with a message.
     *
     * @param message the error message
     */
    public KeyEnvException(String message) {
        this(0, message, null);
    }

    /**
     * Creates a new KeyEnvException with a message and cause.
     *
     * @param message the error message
     * @param cause the cause
     */
    public KeyEnvException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
        this.code = null;
    }

    /**
     * Creates a new KeyEnvException with HTTP status and message.
     *
     * @param status the HTTP status code
     * @param message the error message
     */
    public KeyEnvException(int status, String message) {
        this(status, message, null);
    }

    /**
     * Creates a new KeyEnvException with HTTP status, message, and error code.
     *
     * @param status the HTTP status code
     * @param message the error message
     * @param code the error code
     */
    public KeyEnvException(int status, String message, String code) {
        super(formatMessage(status, message, code));
        this.status = status;
        this.code = code;
    }

    private static String formatMessage(int status, String message, String code) {
        if (status == 0) {
            return "keyenv: " + message;
        }
        if (code != null && !code.isEmpty()) {
            return String.format("keyenv: %s (status=%d, code=%s)", message, status, code);
        }
        return String.format("keyenv: %s (status=%d)", message, status);
    }

    /**
     * Gets the HTTP status code.
     *
     * @return the status code, or 0 if not an HTTP error
     */
    public int getStatus() {
        return status;
    }

    /**
     * Gets the error code from the API response.
     *
     * @return the error code, or null if not available
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns true if this is a 404 Not Found error.
     *
     * @return true if not found
     */
    public boolean isNotFound() {
        return status == 404;
    }

    /**
     * Returns true if this is a 401 Unauthorized error.
     *
     * @return true if unauthorized
     */
    public boolean isUnauthorized() {
        return status == 401;
    }

    /**
     * Returns true if this is a 403 Forbidden error.
     *
     * @return true if forbidden
     */
    public boolean isForbidden() {
        return status == 403;
    }

    /**
     * Returns true if this is a 409 Conflict error.
     *
     * @return true if conflict
     */
    public boolean isConflict() {
        return status == 409;
    }

    /**
     * Returns true if this is a 429 Too Many Requests error.
     *
     * @return true if rate limited
     */
    public boolean isRateLimited() {
        return status == 429;
    }

    /**
     * Returns true if this is a 5xx server error.
     *
     * @return true if server error
     */
    public boolean isServerError() {
        return status >= 500 && status < 600;
    }
}
