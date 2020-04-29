package eu.europeana.metis.sandbox.repository.projection;

import eu.europeana.metis.sandbox.common.Step;

public class RecordView {

  private final Long id;
  private final String recordId;
  private final String datasetId;
  private final Step step;
  private final String errorMessage;

  public RecordView(Long id, String recordId, String datasetId,
      Step step, String errorMessage) {
    this.id = id;
    this.recordId = recordId;
    this.datasetId = datasetId;
    this.step = step;
    this.errorMessage = errorMessage;
  }

  public Long getId() {
    return id;
  }

  public String getRecordId() {
    return recordId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public Step getStep() {
    return step;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
