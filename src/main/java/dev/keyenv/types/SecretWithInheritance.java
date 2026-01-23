package dev.keyenv.types;

/**
 * Represents a secret with inheritance information.
 */
public class SecretWithInheritance extends Secret {

    private String inheritedFrom;

    public SecretWithInheritance() {}

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
        return "SecretWithInheritance{" +
            "id='" + getId() + '\'' +
            ", key='" + getKey() + '\'' +
            ", inheritedFrom='" + inheritedFrom + '\'' +
            '}';
    }
}
