package dev.keyenv.types;

/**
 * Result of a bulk import operation.
 */
public class BulkImportResult {

    private int created;
    private int updated;
    private int skipped;

    public BulkImportResult() {}

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    /**
     * Returns the total number of secrets processed.
     *
     * @return total processed
     */
    public int getTotal() {
        return created + updated + skipped;
    }

    @Override
    public String toString() {
        return "BulkImportResult{" +
            "created=" + created +
            ", updated=" + updated +
            ", skipped=" + skipped +
            '}';
    }
}
