package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a warning exception encountered during the processing of an execution record.
 *
 * <p>Stores details of the associated execution record, warning message, and exception details.
 * <p>Linked to the ExecutionRecord entity through a many-to-one relationship.
 * <p>This entity is indexed on datasetId and executionId for efficient querying.
 */
@Entity
@Table(schema = "engine_record", indexes = {
    @Index(name = "exec_rec_warning_dataset_id_execution_id_idx", columnList = "datasetId, executionId")
})
@Getter
@Setter
public class ExecutionRecordWarning {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "datasetId", referencedColumnName = "datasetId")
  @JoinColumn(name = "executionId", referencedColumnName = "executionId")
  @JoinColumn(name = "sourceRecordId", referencedColumnName = "sourceRecordId")
  @JoinColumn(name = "recordId", referencedColumnName = "recordId")
  @JoinColumn(name = "executionName", referencedColumnName = "executionName")
  private ExecutionRecord executionRecord;

  @Column(columnDefinition = "TEXT")
  private String message;

  @Column(columnDefinition = "TEXT")
  private String exception;
}
