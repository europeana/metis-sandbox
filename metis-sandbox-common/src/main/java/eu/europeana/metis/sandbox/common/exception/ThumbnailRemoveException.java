package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;
import static java.lang.String.join;

import java.util.List;
import lombok.experimental.StandardException;

/**
 * Exception indicating an error occurred while removing thumbnails.
 */
@StandardException
public class ThumbnailRemoveException extends ServiceException {

  /**
   * Constructor.
   * <p>
   * The message includes a formatted list of thumbnail IDs that could not be removed.
   *
   * @param thumbnailIds the list of thumbnail IDs that triggered the exception.
   * @param cause the underlying cause of this exception, used for debugging and logging purposes.
   */
  public ThumbnailRemoveException(List<String> thumbnailIds, Throwable cause) {
    super(format("Error removing thumbnails: [%s]. ", join(",", thumbnailIds)),
        cause);
  }
}
