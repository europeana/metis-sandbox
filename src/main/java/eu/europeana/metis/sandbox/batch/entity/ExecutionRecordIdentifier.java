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

  /**
   * Represents an external identifier retrieved from a provider.
   * <p>For example, an oai-pmh identifier or a file path.</p>
   */
  @Column(length = 300)
  private String externalRecordId;

  /**
   * Represents a source identifier retrieved from inside the record data.
   * <p>For example, the provided cho about</p>
   */
  @Column(length = 300)
  private String sourceRecordId;

  /**
   * Represents a record identifier generated/transformed from the source identifier.
   * <p>For example, the converted provided cho about</p>
   */
  @Column(length = 300)
  private String recordId;
}
