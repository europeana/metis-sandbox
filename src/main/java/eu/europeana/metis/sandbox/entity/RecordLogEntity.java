package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * Entity to map to record_log table
 */
@Entity
@Table(name = "record_log")
public class RecordLogEntity extends RecordEntity {

  private Integer recordId;

  @Enumerated(EnumType.STRING)
  protected Step step;

  @Enumerated(EnumType.STRING)
  protected Status status;

  /**
   * Parameterized constructor
   * @param recordId the record id
   * @param step the workflow step
   * @param status the status of the record
   */

  public RecordLogEntity(Integer recordId, Step step, Status status) {
    this.recordId = recordId;
    this.step = step;
    this.status = status;

  }

  public RecordLogEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public Integer getRecordId() {
    return recordId;
  }

  public void setRecordId(Integer recordId) {
    this.recordId = recordId;
  }

  public Step getStep() {
    return step;
  }

  public void setStep(Step step) {
    this.step = step;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
