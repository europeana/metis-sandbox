package eu.europeana.metis.sandbox.common;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum to represent a record event processing status
 */
public enum Status {
  SUCCESS("success"),
  FAIL("error"),
  WARN("warning");

  private final String value;

  Status(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }
}
