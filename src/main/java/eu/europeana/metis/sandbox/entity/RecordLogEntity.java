package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

/**
 * Entity to map to record_log table
 */
@Entity
@Table(name = "record_log")
@SecondaryTable(name = "record", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id"))

public class RecordLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long recordId;

  @Column(name = "dataset_id", table = "record")
  private String datasetId;

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

  public RecordLogEntity(Long recordId, Step step, Status status) {
    this.recordId = recordId;
    this.step = step;
    this.status = status;

  }

  public RecordLogEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public Long getRecordId() {
    return recordId;
  }

  public void setRecordId(Long recordId) {
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

  public void setId(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }
}
