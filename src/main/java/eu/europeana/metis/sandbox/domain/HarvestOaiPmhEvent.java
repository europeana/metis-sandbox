package eu.europeana.metis.sandbox.domain;


import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;

public class HarvestOaiPmhEvent {

  //  private RecordInfo recordInfo;
  private Status status;
  private Step step;
  private String datasetId;
  private String oaiRecordId;
  private String url;
  private String setspec;
  private String metadataformat;


  public HarvestOaiPmhEvent(Status status, Step step, String url, String setspec,
      String metadataformat, String oaiRecordId, String datasetId) {
//    this.recordInfo = recordInfo;
    this.status = status;
    this.step = step;
    this.url = url;
    this.setspec = setspec;
    this.metadataformat = metadataformat;
    this.oaiRecordId = oaiRecordId;
    this.datasetId = datasetId;
  }

//  public RecordInfo getRecordInfo() {
//    return recordInfo;
//  }

//  public void setRecordInfo(RecordInfo recordInfo) {
//    this.recordInfo = recordInfo;
//  }

//  public Status getStatus() {
//    return status;
//  }
//
//  public void setStatus(Status status) {
//    this.status = status;
//  }

//  public List<RecordError> getRecordErrors() {
//    return recordInfo.getErrors();
//  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }


  public void setSetspec(String setspec) {
    this.setspec = setspec;
  }

  public String getSetspec() {
    return setspec;
  }


  public void setMetadataformat(String metadataformat) {
    this.metadataformat = metadataformat;
  }

  public String getMetadataformat() {
    return metadataformat;
  }

  public void setOaiRecordId(String oaiRecordId) {
    this.oaiRecordId = oaiRecordId;
  }

  public String getOaiRecordId() {
    return oaiRecordId;
  }


  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Step getStep() {
    return step;
  }

  public void setStep(Step step) {
    this.step = step;
  }
}
