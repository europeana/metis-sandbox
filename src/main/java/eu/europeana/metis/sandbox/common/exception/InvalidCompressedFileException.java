package eu.europeana.metis.sandbox.common.exception;

public class InvalidCompressedFileException extends RuntimeException {

  private static final long serialVersionUID = -2555540887797325483L;

  public InvalidCompressedFileException(Throwable cause) {
    super("File provided is not valid compressed file. ", cause);
  }

}
