package eu.europeana.metis.sandbox.domain;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * Event that contains a record and its processing details
 */
public class RecordProcessEvent {

  private final RecordInfo recordInfo;
  private final Status status;
  private final Step step;
  private int maxRecords;
  private String url;
  private String setspec;
  private String metadataformat;
  private MultipartFile xsltFile;

  /**
   * Creates an event based on the one provided, using the provided {@link Step}
   *
   * @param recordInfo must not be null
   * @param step       must not be null
   * @param status     must not be null
   */
  public RecordProcessEvent(RecordInfo recordInfo, Step step, Status status, int maxRecords,
      String url, String setspec, String metadataformat, MultipartFile xsltFile) {
    this.status = status;
    this.recordInfo = recordInfo;
    this.step = step;
    this.maxRecords = maxRecords;
    this.url = url;
    this.setspec = setspec;
    this.metadataformat = metadataformat;
    this.xsltFile = xsltFile;
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

  public int getMaxRecords() {
    return maxRecords;
  }

  public void setMaxRecords(int maxRecords) {
    this.maxRecords = maxRecords;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getSetspec() {
    return setspec;
  }

  public void setSetspec(String setspec) {
    this.setspec = setspec;
  }

  public String getMetadataformat() {
    return metadataformat;
  }

  public void setMetadataformat(String metadataformat) {
    this.metadataformat = metadataformat;
  }

  public MultipartFile getXsltFile() {
    return xsltFile;
  }

  public void setXsltFile(MultipartFile xsltFile) {
    this.xsltFile = xsltFile;
  }
}
