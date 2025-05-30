package eu.europeana.metis.sandbox.entity.projection;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;

public class ErrorLogViewImpl implements ErrorLogView {

  private final Long id;
  private final String recordId;
  private final Step step;
  private final Status status;
  private final String message;

  public ErrorLogViewImpl(Long id, String recordId,
      Step step, Status status, String message) {
    this.id = id;
    this.recordId = recordId;
    this.step = step;
    this.status = status;
    this.message = message;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public String getRecordId() {
    return recordId;
  }

  @Override
  public Step getStep() {
    return step;
  }

  @Override
  public Status getStatus() {
    return status;
  }

  @Override
  public String getMessage() {
    return message;
  }
}