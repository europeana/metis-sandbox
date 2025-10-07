package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an identifier used to uniquely define an execution record.
 * <p>
 * This class serves as an embeddable entity shared across multiple related entities, providing consistent identification fields
 * for execution record management.
 */
@Getter
@Setter
@Embeddable
public class ExecutionRecordIdentifier {

  @Column(length = 300)
  private String externalRecordId;

  @Column(length = 300)
  private String sourceRecordId;

  @Column(length = 300)
  private String recordId;
}
