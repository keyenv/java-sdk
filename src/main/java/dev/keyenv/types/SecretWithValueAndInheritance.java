package dev.keyenv.types;

/**
 * Represents a secret with value and inheritance information.
 */
public class SecretWithValueAndInheritance extends Secret {

    private String value;
    private String inheritedFrom;

    public SecretWithValueAndInheritance() {}

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getInheritedFrom() {
        return inheritedFrom;
    }

    public void setInheritedFrom(String inheritedFrom) {
        this.inheritedFrom = inheritedFrom;
    }

    /**
     * Returns true if this secret is inherited from another environment.
     *
     * @return true if inherited
     */
    public boolean isInherited() {
        return inheritedFrom != null && !inheritedFrom.isEmpty();
    }

    @Override
    public String toString() {
        return "SecretWithValueAndInheritance{" +
            "id='" + getId() + '\'' +
            ", key='" + getKey() + '\'' +
            ", value='[REDACTED]'" +
            ", inheritedFrom='" + inheritedFrom + '\'' +
            '}';
    }
}
