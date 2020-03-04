package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class Dataset {

  private final String datasetId;

  @EqualsAndHashCode.Exclude
  private final List<Record> records;

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
}
