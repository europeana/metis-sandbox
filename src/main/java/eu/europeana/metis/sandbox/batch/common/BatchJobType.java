package eu.europeana.metis.sandbox.batch.common;

import java.util.Optional;

public enum BatchJobType {
  OAI_HARVEST,
  FILE_HARVEST,
  VALIDATION(ValidationBatchJobSubType.class),
  TRANSFORMATION(TransformationBatchJobSubType.class),
  NORMALIZATION,
  ENRICHMENT,
  MEDIA,
  INDEX,
  DEBIAS;

  private final Class<? extends Enum<?>> subTypeClass;

  BatchJobType() {
    this.subTypeClass = null;
  }

  BatchJobType(Class<? extends Enum<?>> subTypeClass) {
    this.subTypeClass = subTypeClass;
  }

  public Optional<Class<? extends Enum<?>>> getSubTypeClass() {
    return Optional.ofNullable(subTypeClass);
  }
}
