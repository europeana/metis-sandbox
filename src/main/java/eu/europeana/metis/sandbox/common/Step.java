package eu.europeana.metis.sandbox.common;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum to represent a record event workflow step
 */
public enum Step {
  HARVEST_OAI_PMH("harvest_oai_pmh",0),
  CREATE("import", 1),
  TRANSFORM_TO_EDM_EXTERNAL("transform to EDM external", 2),
  VALIDATE_EXTERNAL("validate (edm external)", 3),
  TRANSFORM("transform", 4),
  VALIDATE_INTERNAL("validate (edm internal)", 5),
  NORMALIZE("normalise", 6),
  ENRICH("enrich", 7),
  MEDIA_PROCESS("process media", 8),
  PREVIEW("preview", 9),
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
