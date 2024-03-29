package eu.europeana.metis.sandbox.common.aggregation;

import eu.europeana.metis.sandbox.repository.RecordRepository;

/**
 * POJO used by {@link RecordRepository} in method getDatasetStatistics, to map query results
 */
public class DatasetStatistic {

  private final String datasetId;

  private final Long count;

  public DatasetStatistic(String datasetId, Long count) {
    this.datasetId = datasetId;
    this.count = count;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public Long getCount() {
    return count;
  }
}
