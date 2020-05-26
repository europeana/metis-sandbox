package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Set;

/**
 * Immutable object that represents a dataset
 * <br /><br />
 * dataset id is represented as a String to keep consistency with other metis tools
 */
public class Dataset {

  private final String datasetId;

  private final Set<Record> records;

  private final int duplicates;

  /**
   * Constructor
   *
   * @param datasetId must not be null
   * @param records   must not be null
   * @param duplicates amount found in the given dataset
   * @throws NullPointerException if any parameter is null
   */
  public Dataset(String datasetId, Set<Record> records, int duplicates) {
    this.duplicates = duplicates;
    requireNonNull(datasetId, "DatasetId must not be null");
    requireNonNull(records, "records must not be null");
    this.datasetId = datasetId;
    this.records = Collections.unmodifiableSet(records);
  }

  public String getDatasetId() {
    return datasetId;
  }

  public Set<Record> getRecords() {
    return records;
  }

  public int getDuplicates() {
    return duplicates;
  }
}
