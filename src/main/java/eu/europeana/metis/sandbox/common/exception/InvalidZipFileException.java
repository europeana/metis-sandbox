package eu.europeana.metis.sandbox.common.exception;

public class InvalidZipFileException extends RuntimeException {

  private static final long serialVersionUID = -2555540887797325483L;

  public InvalidZipFileException(Throwable cause) {
    super("File provided is not valid zip", cause);
  }

}
