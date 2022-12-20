package eu.europeana.metis.sandbox.common.exception;

/**
 * Exception for when a record is duplicated
 */
public class RecordDuplicatedException extends ServiceException {

  private static final long serialVersionUID = -62038112413570301L;

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
   */
  public RecordDuplicatedException(String message) {
    super(message, null);
  }
}
