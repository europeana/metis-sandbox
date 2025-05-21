package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "batch-framework", indexes = {
    @Index(name = "exec_rec_warning_exception_dataset_id_execution_id_idx", columnList = "datasetId, executionId")
})
@Getter
@Setter
public class ExecutionRecordWarningException {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
      @JoinColumn(name = "datasetId", referencedColumnName = "datasetId"),
      @JoinColumn(name = "executionId", referencedColumnName = "executionId"),
      @JoinColumn(name = "recordId", referencedColumnName = "recordId"),
      @JoinColumn(name = "executionName", referencedColumnName = "executionName")
  })
  private ExecutionRecord executionRecord;

  @Column(columnDefinition = "TEXT")
  private String message;

  @Column(columnDefinition = "TEXT")
  private String exception;
}
