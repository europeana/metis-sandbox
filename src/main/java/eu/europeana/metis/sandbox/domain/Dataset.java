package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Immutable object that represents a dataset
 */
public class Dataset {

  private final String datasetId;

  private final List<Record> records;

  /**
   * Constructor
   * @param datasetId must not be null
   * @param records must not be null
   * @throws NullPointerException if any parameter is null
   */
  public Dataset(String datasetId, List<Record> records) {
    requireNonNull(datasetId, "DatasetId must not be null");
    requireNonNull(records, "records must not be null");
    this.datasetId = datasetId;
    this.records = Collections.unmodifiableList(records);
  }

  public String getDatasetId() {
    return datasetId;
  }

  public List<Record> getRecords() {
    return records;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Dataset dataset = (Dataset) o;
    return datasetId.equals(dataset.datasetId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datasetId);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Dataset.class.getSimpleName() + "[", "]")
        .add("datasetId='" + datasetId + "'")
        .add("records=" + records)
        .toString();
  }
}
