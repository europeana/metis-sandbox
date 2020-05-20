package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;
import static java.lang.String.join;

import java.util.List;

public class ThumbnailRemoveException extends ServiceException {

  private static final long serialVersionUID = -3507180122567870480L;

  public ThumbnailRemoveException(List<String> thumbnailIds, Throwable cause) {
    super(format("Error removing thumbnails: %s. %s ", join(",", thumbnailIds), cause.getMessage()),
        cause);
  }

  public ThumbnailRemoveException(Throwable cause) {
    super("Error removing thumbnails " + cause.getMessage(), cause);
  }
}
