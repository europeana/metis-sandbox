package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a composite key used to uniquely identify an external identifier associated with an execution record.
 * <p>The class is embeddable within entities to provide database support for composite keys.
 * <p>Implements ExecutionRecordIdAccess to allow standardized access to identifier fields.
 */
@Getter
@Setter
@Embeddable
public class ExecutionRecordExternalIdentifierKey implements ExecutionRecordIdAccess {

  @Column(length = 50)
  private String datasetId;

  @Column(length = 50)
  private String executionId;

  @Column(length = 50)
  private String executionName;

  @Column(length = 300)
  private String sourceRecordId;
}
