package eu.europeana.metis.sandbox.domain;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Getter
public class Event<T extends Record> {

  @NonNull
  private final T body;
  @NonNull
  private final Status status;
  @NonNull
  private final Step step;

  private final String exception;

  public Event(T body, Step step) {
    this.status = Status.SUCCESS;
    this.body = body;
    this.exception = null;
    this.step = step;
  }

  public Event(T body, Step step, Exception exception) {
    this.status = exception == null ? Status.SUCCESS : Status.FAIL;
    this.body = body;
    this.exception = exception == null ? null : ExceptionUtils.getStackTrace(exception);
    this.step = step;
  }

  public Event(T body, Step step, String exception) {
    this.status = exception == null ? Status.SUCCESS : Status.FAIL;
    this.body = body;
    this.exception = exception;
    this.step = step;
  }
}
