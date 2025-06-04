package eu.europeana.metis.sandbox.common;

/**
 * Enum class to represent the type of harvesting
 */
public enum HarvestProtocol {
    FILE("HARVEST_FILE"),
    HTTP("HARVEST_HTTP"),
    OAI("HARVEST_OAI_PMH");
    private final String value;

    HarvestProtocol(String value) {
        this.value = value;
    }

    /**
     * Returns the enum value as a String
     * @return a String value of the enum
     */
    public String value() {
        return value;
    }
}
