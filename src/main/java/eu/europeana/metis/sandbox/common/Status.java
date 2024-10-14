package eu.europeana.metis.sandbox.common;


/**
 * Enum to represent a record event processing status
 */
public enum Status {
  SUCCESS("Success"),
  FAIL("Error"),
  WARN("Warning"),
  PENDING("Pending");
  private final String value;

  Status(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
