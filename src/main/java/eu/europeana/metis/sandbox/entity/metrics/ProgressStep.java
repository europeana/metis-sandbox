package eu.europeana.metis.sandbox.entity.metrics;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity class for keep track of dataset step metrics
 */
@Entity
@Table(schema="metrics", name="progress_per_step")
public class ProgressStep {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long metric_id;

  @Column(name = "dataset_id", nullable = false)
  private Long datasetId;

  @Column(name = "step", nullable = false)
  private String step;

  @Column(name = "total", nullable = false)
  private Long total;

  @Column(name = "success", nullable = false)
  private Long success;

  @Column(name = "fail", nullable = false)
  private Long fail;

  @Column(name = "warn", nullable = false)
  private Long warn;

  public Long getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Long datasetId) {
    this.datasetId = datasetId;
  }

  public String getStep() {
    return step;
  }

  public void setStep(String step) {
    this.step = step;
  }

  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  public Long getSuccess() {
    return success;
  }

  public void setSuccess(Long success) {
    this.success = success;
  }

  public Long getFail() {
    return fail;
  }

  public void setFail(Long fail) {
    this.fail = fail;
  }

  public Long getWarn() {
    return warn;
  }

  public void setWarn(Long warn) {
    this.warn = warn;
  }
}
