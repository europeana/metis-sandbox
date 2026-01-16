package eu.europeana.metis.sandbox.common.exception;

import java.io.Serial;

public class RecordParsingException extends ServiceException {

  @Serial
  private static final long serialVersionUID = -8418950607860063126L;

  public RecordParsingException(Throwable cause) {
    super("Error while parsing a xml record. ", cause);
  }
}
