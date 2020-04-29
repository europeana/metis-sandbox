package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "record_error_log")
public class RecordErrorLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  private RecordLogEntity record;

  private String datasetId;

  private Step step;

  private Status status;

  private String message;

  private String stackTrace;

  public RecordErrorLogEntity(RecordLogEntity record, Step step, String message,
      String stackTrace) {
    this.record = record;
    this.step = step;
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

  public RecordLogEntity getRecord() {
    return record;
  }

  public void setRecord(RecordLogEntity recordId) {
    this.record = recordId;
  }

  public Step getStep() {
    return step;
  }

  public void setStep(Step step) {
    this.step = step;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordErrorLogEntity that = (RecordErrorLogEntity) o;
    return Objects.equals(id, that.id) &&
        record.equals(that.record) &&
        step == that.step &&
        message.equals(that.message) &&
        stackTrace.equals(that.stackTrace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, record, step, message, stackTrace);
  }
}
