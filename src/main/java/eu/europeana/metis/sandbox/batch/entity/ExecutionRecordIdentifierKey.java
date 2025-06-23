package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a composite key to uniquely identify an execution record.
 *
 * <p>Implements ExecutionRecordIdAccess to standardize access to these identifier fields.
 */
@Getter
@Setter
@Embeddable
public class ExecutionRecordIdentifierKey implements ExecutionRecordIdAccess {

  @Column(length = 50)
  private String datasetId;

  @Column(length = 50)
  private String executionId;

  @Column(length = 50)
  private String executionName;

  @Column(length = 300)
  private String sourceRecordId;

  @Column(length = 300)
  private String recordId;
}
