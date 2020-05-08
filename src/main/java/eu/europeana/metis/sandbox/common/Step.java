package eu.europeana.metis.sandbox.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Step {
  CREATE("create", 0),
  VALIDATE_EXTERNAL("external validation", 1),
  TRANSFORM("transform", 2),
  VALIDATE_INTERNAL("internal validation", 3),
  NORMALIZE("normalization", 4),
  ENRICH("enrichment", 5),
  MEDIA_PROCESS("media processing", 6),
  INDEX("indexing", 7),
  FINISH("finish", 8);

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
