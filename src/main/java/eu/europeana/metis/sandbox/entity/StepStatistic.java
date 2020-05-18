package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;

public class StepStatistic {

  private final Step step;

  private final Status status;

  private final long count;

  public StepStatistic(Step step, Status status, long count) {
    this.step = step;
    this.status = status;
    this.count = count;
  }

  public Step getStep() {
    return step;
  }

  public Status getStatus() {
    return status;
  }

  public long getCount() {
    return count;
  }
}
