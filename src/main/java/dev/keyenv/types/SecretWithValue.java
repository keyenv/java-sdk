package dev.keyenv.types;

/**
 * Represents a secret including its decrypted value.
 */
public class SecretWithValue extends Secret {

    private String value;

    public SecretWithValue() {}

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "SecretWithValue{" +
            "id='" + getId() + '\'' +
            ", key='" + getKey() + '\'' +
            ", value='[REDACTED]'" +
            '}';
    }
}
