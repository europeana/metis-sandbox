package eu.europeana.metis.sandbox.common;

/**
 * Enum class to represent the type of harvesting
 */
public enum Protocol {
    FILE("FILE"),
    HTTP("HTTP"),
    OAI_PMH("OAI-PMH");
    private final String value;

    Protocol(String value) {
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
