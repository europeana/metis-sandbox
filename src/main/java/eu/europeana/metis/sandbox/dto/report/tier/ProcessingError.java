package eu.europeana.metis.sandbox.dto.report.tier;

class ProcessingError {

  private String errorMessage;
  private int errorCode;

  public ProcessingError() {
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }
}
