package eu.europeana.metis.sandbox.common.exception;

/**
 * Exception for when a record is duplicated
 */
public class RecordDuplicatedException extends RecordProcessingException {

  private static final long serialVersionUID = -62038112413570301L;

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
   * @param recordId the id of the record that threw the exception
   */
  public RecordDuplicatedException(String message, String recordId, String providerId, String europeanaId) {
    super("ProviderId: " + providerId + " | EuropeanaId: " + europeanaId + " is duplicated.", recordId, message, null);
  }
}
