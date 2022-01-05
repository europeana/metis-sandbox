package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Entity to map to record_error_log table
 */
@Entity
@Table(name = "record_error_log")
public class RecordErrorLogEntity extends RecordEntity {

  private String message;

  private String stackTrace;

  public RecordErrorLogEntity(String recordId, String europeanaId, String datasetId,
      Step step, Status status, String message, String stackTrace) {
    this.recordId = recordId;
    this.europeanaId = europeanaId;
    this.datasetId = datasetId;
    this.step = step;
    this.status = status;
    this.message = message;
    this.stackTrace = stackTrace;
  }

  public RecordErrorLogEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
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
}
