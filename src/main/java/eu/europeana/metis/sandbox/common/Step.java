package eu.europeana.metis.sandbox.common;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum to represent a record event workflow step
 */
public enum Step {
  CREATE("import", 0),
  VALIDATE_EXTERNAL("validate (edm external)", 1),
  TRANSFORM("transform", 2),
  VALIDATE_INTERNAL("validate (edm internal)", 3),
  NORMALIZE("normalise", 4),
  ENRICH("enrich", 5),
  MEDIA_PROCESS("process media", 6),
  PREVIEW("preview", 7),
  PUBLISH("publish", 8),
  CLOSE("close", 9);

  private final String value;
  private final int precedence;

  Step(String value, int precedence) {
    this.value = value;
    this.precedence = precedence;
  }

  @JsonValue
  public String value() {
    return value;
  }

  public int precedence() {
    return precedence;
  }
}
