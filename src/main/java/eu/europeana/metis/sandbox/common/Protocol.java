package eu.europeana.metis.sandbox.common;

public enum Protocol {
    FILE("FILE"),
    HTTP("HTTP"),
    OAI_PMH("OAI-PMH");
    private final String value;

    Protocol(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
