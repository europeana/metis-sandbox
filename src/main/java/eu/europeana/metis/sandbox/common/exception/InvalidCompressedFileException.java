package eu.europeana.metis.sandbox.common.exception;

/**
 * Exception class for when an invalid type of compressed file is detected
 */
public class InvalidCompressedFileException extends RuntimeException {

  private static final long serialVersionUID = -2555540887797325483L;

  public InvalidCompressedFileException(Throwable cause) {
    super("File provided is not valid compressed file. ", cause);
  }

}
