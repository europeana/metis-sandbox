package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class Event {

  private final Record body;
  private final Status status;
  private final Step step;

  private final Exception exception;

  public Event(Record body, Step step) {
    this(body, step, null);
  }

  public Event(Record body, Step step, Exception exception) {
    requireNonNull(body, "Body must not be null");
    requireNonNull(step, "Step must not be null");
    this.status = exception == null ? Status.SUCCESS : Status.FAIL;
    this.body = body;
    this.exception = exception;
    this.step = step;
  }

  public Record getBody() {
    return body;
  }

  public Status getStatus() {
    return status;
  }

  public Step getStep() {
    return step;
  }

  public Exception getException() {
    return exception;
  }

  public String getExceptionStackTrace() {
    return exception == null ? null : ExceptionUtils.getStackTrace(exception);
  }
}
