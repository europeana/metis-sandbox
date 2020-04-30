package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;

/**
 * Immutable object that represents a dataset
 */
public class Dataset {

  private final Integer datasetId;

  private final List<Record> records;

  /**
   * Constructor
   *
   * @param datasetId must not be null
   * @param records   must not be null
   * @throws NullPointerException if any parameter is null
   */
  public Dataset(Integer datasetId, List<Record> records) {
    requireNonNull(datasetId, "DatasetId must not be null");
    requireNonNull(records, "records must not be null");
    this.datasetId = datasetId;
    this.records = Collections.unmodifiableList(records);
  }

  public Integer getDatasetId() {
    return datasetId;
  }

  public List<Record> getRecords() {
    return records;
  }
}
