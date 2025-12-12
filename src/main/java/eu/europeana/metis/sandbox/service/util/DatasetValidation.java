package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.common.exception.DatasetFileSizeException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/**
 * The type Dataset validation.
 */
public final class DatasetValidation {

  /**
   * The constant DEFAULT_MAX_FILE_SIZE. define a default maximum file size to 64 MB for http harvest
   */
  public static final Long DEFAULT_MAX_FILE_SIZE = 64L * 1024L * 1024L; // 64 MB

  private DatasetValidation() {
    // Utility class
  }

  /**
   * Check url content length.
   *
   * @param url the url
   * @throws IllegalArgumentException the illegal argument exception if content length exceeds the maximum
   */
  public static void checkUrlContentLength(String url) {
    try {
      URL urlObj = new URI(url).toURL();
      URLConnection conn = urlObj.openConnection();

      long size = conn.getContentLengthLong();

      if (size > 0 && size > DEFAULT_MAX_FILE_SIZE) {
        throw new DatasetFileSizeException("File content too large: " + size + " bytes, "
            + "max allowed: " + DEFAULT_MAX_FILE_SIZE + " bytes");
      }
    } catch (IOException | URISyntaxException e) {
      throw new ServiceException("Invalid URL: " + url, e);
    }
  }
}
