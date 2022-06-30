package eu.europeana.metis.sandbox.common;


/**
 * Enum to represent a record event processing status
 */
public enum Status {
  SUCCESS("Success"),
  FAIL("Error"),
  WARN("Warning"),
  HARVESTING_IDENTIFIERS("Harvesting Identifiers"),
  COMPLETED("Completed"),
  IN_PROGRESS("In Progress");

  private final String value;

  Status(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
