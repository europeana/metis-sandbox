package eu.europeana.metis.sandbox.domain;

//import eu.europeana.metis.sandbox.common.Status;
//import eu.europeana.metis.sandbox.common.Step;
//import eu.europeana.metis.sandbox.common.locale.Country;
//import eu.europeana.metis.sandbox.common.locale.Language;
//import org.springframework.web.multipart.MultipartFile;
//
//public class HarvestOaiPmhEvent extends RecordProcessEvent {
//
//  private int maxRecords;
//  private String url;
//  private String setspec;
//  private String metadataformat;
//  private MultipartFile xsltFile;
//
//
//  public HarvestOaiPmhEvent(RecordInfo recordInfo, Step step, Status status, int maxRecords,
//      String url, String setspec, String metadataformat, MultipartFile xsltFile) {
//    super(recordInfo, step, status);
//    this.maxRecords = maxRecords;
//    this.url = url;
//    this.setspec = setspec;
//    this.metadataformat = metadataformat;
//    this.xsltFile = xsltFile;
//  }
//
//  public int getMaxRecords() {
//    return maxRecords;
//  }
//
//  public void setMaxRecords(int maxRecords) {
//    this.maxRecords = maxRecords;
//  }
//
//  public String getDataSetName() {
//    return super.getRecord().getDatasetName();
//  }
//
//  public RecordInfo getRecordInfo() {
//    return super.getRecordInfo();
//  }
//
//  public Country getCountry() {
//    return super.getRecord().getCountry();
//  }
//
//  public Language getLanguage() {
//    return super.getRecord().getLanguage();
//  }
//
//  public String getUrl() {
//    return url;
//  }
//
//  public void setUrl(String url) {
//    this.url = url;
//  }
//
//  public String getSetspec() {
//    return setspec;
//  }
//
//  public void setSetspec(String setspec) {
//    this.setspec = setspec;
//  }
//
//  public String getMetadataformat() {
//    return metadataformat;
//  }
//
//  public void setMetadataformat(String metadataformat) {
//    this.metadataformat = metadataformat;
//  }
//
//  public MultipartFile getXsltFile() {
//    return xsltFile;
//  }
//
//  public void setXsltFile(MultipartFile xsltFile) {
//    this.xsltFile = xsltFile;
//  }
//}
