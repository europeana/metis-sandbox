package eu.europeana.metis.sandbox.common.exception;

public class RecordParsingException extends ServiceException {

  private static final long serialVersionUID = -8418950607860063126L;

  public RecordParsingException(Throwable cause) {
    super("Error while parsing a xml record. ", cause);
  }
}
