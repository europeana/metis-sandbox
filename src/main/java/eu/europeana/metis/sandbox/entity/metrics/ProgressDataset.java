package eu.europeana.metis.sandbox.entity.metrics;

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

  @Column(name = "dataset_id", nullable = false)
  private Integer datasetId;

  @Column(name = "total", nullable = false)
  private Integer total;

  @Column(name = "processed", nullable = false)
  private Integer processed;

  public ProgressDataset() {
    //Required for JPA
  }

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public Integer getProcessed() {
    return processed;
  }

  public void setProcessed(Integer processed) {
    this.processed = processed;
  }
}
