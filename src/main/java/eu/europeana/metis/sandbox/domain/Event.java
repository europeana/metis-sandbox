package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import java.util.Collections;
import java.util.List;

/**
 * Event that contains a record and its processing details
 */
public class Event {

  private final Record body;
  private final List<RecordError> recordErrors;
  private final Status status;
  private final Step step;

  /**
   * Creates an event based on the one provided, using the provided {@link Step}
   *
   * @param recordInfo must not be null
   * @param step       must not be null
   * @param status     must not be null
   * @throws NullPointerException if any parameter is null
   */
  public Event(RecordInfo recordInfo, Step step, Status status) {
    requireNonNull(recordInfo, "Record info must not be null");
    requireNonNull(step, "Step must not be null");
    requireNonNull(status, "Status must not be null");
    this.status = status;
    this.body = recordInfo.getRecord();
    this.recordErrors = Collections.unmodifiableList(recordInfo.getErrors());
    this.step = step;
  }

  public Record getBody() {
    return body;
  }

  public List<RecordError> getRecordErrors() {
    return recordErrors;
  }

  public Status getStatus() {
    return status;
  }

  public Step getStep() {
    return step;
  }
}
