package eu.europeana.metis.sandbox.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Step {
  CREATE("create"),
  VALIDATE_EXTERNAL("external validation"),
  TRANSFORM("transform"),
  VALIDATE_INTERNAL("internal validation"),
  NORMALIZE("normalization"),
  ENRICH("enrichment"),
  MEDIA_PROCESS("media processing"),
  INDEX("indexing"),
  CLOSE("close");

  private final String value;

  Step(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }
}
