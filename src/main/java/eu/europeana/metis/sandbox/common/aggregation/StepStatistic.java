package eu.europeana.metis.sandbox.common.aggregation;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.Status;

/**
 * POJO used by {@link RecordLogRepository} in method getStepStatistics, to map query results
 */
public class StepStatistic {

  private final FullBatchJobType step;

  private final Status status;

  private final Long count;

  public StepStatistic(FullBatchJobType step, Status status, Long count) {
    this.step = step;
    this.status = status;
    this.count = count;
  }

  public FullBatchJobType getStep() {
    return step;
  }

  public Status getStatus() {
    return status;
  }

  public Long getCount() {
    return count;
  }
}
