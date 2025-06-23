package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing an external identifier associated with an execution record.
 * <p>
 * It includes a composite key for uniquely identifying the record and a flag indicating if the record is marked as deleted.
 * Mostly used for oai harvesting.
 */
@Getter
@Setter
@Entity
@Table(schema = "engine_record")
public class ExecutionRecordExternalIdentifier implements HasExecutionRecordIdAccess<ExecutionRecordExternalIdentifierKey> {

  @EmbeddedId
  private ExecutionRecordExternalIdentifierKey identifier;
  private boolean isDeleted;
}
