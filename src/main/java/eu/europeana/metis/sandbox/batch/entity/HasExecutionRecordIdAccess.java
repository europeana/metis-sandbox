package eu.europeana.metis.sandbox.batch.entity;

/**
 * Interface for classes that provide access to an execution record identifier.
 *
 * @param <T> Type that extends ExecutionRecordIdAccess, representing the identifier.
 */
public interface HasExecutionRecordIdAccess<T extends ExecutionRecordIdAccess> {

  T getIdentifier();
}
