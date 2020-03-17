package eu.europeana.metis.sandbox.common.exception;

public class InvalidZipFileException extends RuntimeException {

  public InvalidZipFileException(Throwable cause) {
    super("File provided is not valid zip", cause);
  }

}
