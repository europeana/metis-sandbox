package eu.europeana.metis.sandbox.entity.metrics;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity class for keep track of metrics of datasets
 */
@Entity
@Table(schema = "metrics", name = "progress_per_dataset")
public class ProgressDataset {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "metric_id", nullable = false)
  private Long metricId;

  @Column(name = "dataset_id", nullable = false, length = 20)
  private String datasetId;

  @Column(name = "total_records", nullable = false)
  private Long totalRecords;

  @Column(name = "processed_records", nullable = false)
  private Long processedRecords;

  @Column(name="status", nullable = false)
  private String status;

  @Column(name="start_timestamp", nullable = false)
  private LocalDateTime startTimeStamp;

  @Column(name="end_timestamp", nullable = false)
  private LocalDateTime endTimeStamp;

  public ProgressDataset() {
    //Required for JPA
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public Long getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(Long total) {
    this.totalRecords = total;
  }

  public Long getProcessedRecords() {
    return processedRecords;
  }

  public void setProcessedRecords(Long processed) {
    this.processedRecords = processed;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getStartTimeStamp() {
    return startTimeStamp;
  }

  public void setStartTimeStamp(LocalDateTime startTimeStamp) {
    this.startTimeStamp = startTimeStamp;
  }

  public LocalDateTime getEndTimeStamp() {
    return endTimeStamp;
  }

  public void setEndTimeStamp(LocalDateTime endTimeStamp) {
    this.endTimeStamp = endTimeStamp;
  }
}
