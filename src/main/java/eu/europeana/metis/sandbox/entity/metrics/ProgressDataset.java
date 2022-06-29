package eu.europeana.metis.sandbox.entity.metrics;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.checkerframework.checker.units.qual.C;

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

  @Column(name = "dataset_id", nullable = false)
  private Long datasetId;

  @Column(name = "total_records", nullable = false)
  private Long total;

  @Column(name = "processed_records", nullable = false)
  private Long processed;

  @Column(name="status", nullable = false)
  private String status;

  public ProgressDataset() {
    //Required for JPA
  }

  public Long getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Long datasetId) {
    this.datasetId = datasetId;
  }

  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  public Long getProcessed() {
    return processed;
  }

  public void setProcessed(Long processed) {
    this.processed = processed;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
