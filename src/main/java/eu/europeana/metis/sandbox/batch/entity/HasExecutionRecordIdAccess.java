package eu.europeana.metis.sandbox.batch.entity;

public interface HasExecutionRecordIdAccess<T extends ExecutionRecordIdAccess> {
  T getIdentifier();
}
