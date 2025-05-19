package eu.europeana.metis.sandbox.batch.config;

public enum ValidationBatchBatchJobSubType implements BatchJobSubType {
  EXTERNAL, INTERNAL;

  @Override
  public String getName() {
    return name();
  }
}
