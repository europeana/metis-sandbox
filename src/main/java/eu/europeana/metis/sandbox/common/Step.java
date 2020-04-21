package eu.europeana.metis.sandbox.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Step {
  CREATE("Create"),
  VALIDATE_EXTERNAL("External Validation"),
  TRANSFORM("Transform"),
  VALIDATE_INTERNAL("Internal Validation"),
  NORMALIZE("Normalization"),
  ENRICH("Enrichment"),
  MEDIA_PROCESS("Media Processing"),
  INDEX("Indexing"),
  FINISH("Finish");

  private final String value;

  Step(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }
}
