package eu.europeana.metis.sandbox.entity.problempatterns;

/**
 * POJO used by {@link DatasetProblemPattern} in method getStepStatistics, to map query results
 */
public class DatasetProblemPatternStatistic {

  private final String patternId;

  private final Long count;

  public DatasetProblemPatternStatistic(String patternId, Long count) {
    this.patternId = patternId;
    this.count = count;
  }

  public String getPatternId() {
    return patternId;
  }

  public Long getCount() {
    return count;
  }
}
