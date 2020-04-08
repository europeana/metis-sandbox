package eu.europeana.metis.sandbox.common.exception;

public class RecordParsingException extends ServiceException {

  public RecordParsingException(Throwable cause) {
    super("Error while parsing a xml record: ", cause);
  }
}
