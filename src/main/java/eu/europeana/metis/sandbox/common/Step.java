package eu.europeana.metis.sandbox.common;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum to represent a record event workflow step
 */
public enum Step {
  HARVEST_OAI_PMH("harvest OAI-PMH",1),
  CREATE("import", 2),
  TRANSFORM_TO_EDM_EXTERNAL("transform to EDM external", 3),
  VALIDATE_EXTERNAL("validate (edm external)", 4),
  TRANSFORM("transform", 5),
  VALIDATE_INTERNAL("validate (edm internal)", 6),
  NORMALIZE("normalise", 7),
  ENRICH("enrich", 8),
  MEDIA_PROCESS("process media", 9),
  PUBLISH("publish", 10),
  CLOSE("close", 11);

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
