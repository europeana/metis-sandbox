package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entity to map to record_error_log table
 */
@Entity
@Table(name = "record_error_log")
public class RecordErrorLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "record_id", referencedColumnName = "id")
  private RecordEntity recordId;

  private String message;

  private String stackTrace;

  @Enumerated(EnumType.STRING)
  protected Step step;

  @Enumerated(EnumType.STRING)
  protected Status status;

  /**
   * Parameterized constructor
   *
   * @param recordId the record id
   * @param step the workflow step
   * @param status the status of the record
   * @param message the message, usually an error message
   * @param stackTrace the stack trace of the error
   */
  public RecordErrorLogEntity(RecordEntity recordId, Step step, Status status, String message,
      String stackTrace) {
    this.recordId = recordId;
    this.step = step;
    this.status = status;
    this.message = message;
    this.stackTrace = stackTrace;
  }

  public RecordErrorLogEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public RecordEntity getRecordId() {
    return recordId;
  }

  public void setRecordId(RecordEntity recordId) {
    this.recordId = recordId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public void setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
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
