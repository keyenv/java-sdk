package dev.keyenv.types;

/**
 * Options for bulk import operations.
 */
public class BulkImportOptions {

    private boolean overwrite;

    public BulkImportOptions() {}

    public BulkImportOptions(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Creates options with overwrite enabled.
     *
     * @return options with overwrite enabled
     */
    public static BulkImportOptions withOverwrite() {
        return new BulkImportOptions(true);
    }

    /**
     * Creates options with overwrite disabled.
     *
     * @return options with overwrite disabled
     */
    public static BulkImportOptions skipExisting() {
        return new BulkImportOptions(false);
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    @Override
    public String toString() {
        return "BulkImportOptions{" +
            "overwrite=" + overwrite +
            '}';
    }
}
