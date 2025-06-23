package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing an exception encountered during the processing of an execution record.
 *
 * <p>Uses a composite key, {@link ExecutionRecordIdentifierKey}, to uniquely identify the associated execution record.
 */
@Getter
@Setter
@Entity
@Table(schema = "engine_record", indexes = {@Index(name = "exec_rec_error_dataset_id_execution_id_idx", columnList = "datasetId, executionId")})
public class ExecutionRecordError implements HasExecutionRecordIdAccess<ExecutionRecordIdentifierKey> {

  @EmbeddedId
  private ExecutionRecordIdentifierKey identifier;

  @Column(columnDefinition = "TEXT")
  private String message;
  @Column(columnDefinition = "TEXT")
  private String exception;
}
