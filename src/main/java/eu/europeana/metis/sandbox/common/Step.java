package eu.europeana.metis.sandbox.common;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum to represent a record event workflow step
 */
public enum Step {
  CREATE("import", 0),
  TRANSFORM_TO_EDM_EXTERNAL("transform to EDM external", 1),
  VALIDATE_EXTERNAL("validate (edm external)", 2),
  TRANSFORM("transform", 3),
  VALIDATE_INTERNAL("validate (edm internal)", 4),
  NORMALIZE("normalise", 5),
  ENRICH("enrich", 6),
  MEDIA_PROCESS("process media", 7),
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
