package eu.europeana.metis.sandbox.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The type Dataset file size exception.
 */
@ResponseStatus(value = HttpStatus.PAYLOAD_TOO_LARGE, reason = "Dataset file size exceeded maximum allowed limit")
public class DatasetFileSizeException extends ServiceException {

  private static final long serialVersionUID = 7914049398400398320L;

  /**
   * Instantiates a new Dataset file size exception.
   *
   * @param message the message
   */
  public DatasetFileSizeException(String message) {
    super(message);
  }
}
