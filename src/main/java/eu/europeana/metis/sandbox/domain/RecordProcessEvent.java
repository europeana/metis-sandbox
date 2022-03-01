package eu.europeana.metis.sandbox.domain;

import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import java.util.List;

/**
 * Event that contains a record and its processing details
 */
public class RecordProcessEvent {

  private final RecordInfo recordInfo;
  private final Status status;
  private final Step step;
  private final OaiHarvestData oaiHarvestData;

  /**
   * Creates an event based on the one provided, using the provided {@link Step}
   *
   * @param recordInfo     must not be null
   * @param step           must not be null
   * @param status         must not be null
   * @param oaiHarvestData class that encapsulates OAI-PMH data
   */
  public RecordProcessEvent(RecordInfo recordInfo, Step step, Status status,
      OaiHarvestData oaiHarvestData) {
    this.status = status;
    this.recordInfo = recordInfo;
    this.step = step;
    this.oaiHarvestData = oaiHarvestData;
  }

  public RecordInfo getRecordInfo() {
    return recordInfo;
  }

  public Record getRecord() {
    return recordInfo.getRecord();
  }

  public List<RecordError> getRecordErrors() {
    return recordInfo.getErrors();
  }

  public Status getStatus() {
    return status;
  }

  public Step getStep() {
    return step;
  }

  public OaiHarvestData getOaiHarvestData() {
    return oaiHarvestData;
  }
}
