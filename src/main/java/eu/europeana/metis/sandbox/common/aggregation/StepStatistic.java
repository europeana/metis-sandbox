package eu.europeana.metis.sandbox.common.aggregation;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;

/**
 * POJO used by {@link RecordLogRepository} in method getStepStatistics, to map query results
 */
public class StepStatistic {

  private final Step step;

  private final Status status;

  private final Long count;

  public StepStatistic(Step step, Status status, Long count) {
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

  public Long getCount() {
    return count;
  }
}
