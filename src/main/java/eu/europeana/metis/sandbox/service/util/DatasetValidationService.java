package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * The type Dataset validation.
 */
@Service
public final class DatasetValidationService {

  @Value("${spring.servlet.multipart.max-file-size}")
  private DataSize defaultMaxFileSize;

  /**
   * Check url content length.
   *
   * @param url the url
   * @throws MaxUploadSizeExceededException the max upload size exceeded exception
   */
  public void checkUrlContentLength(String url) {
    try {
      URL urlObj = new URI(url).toURL();
      URLConnection conn = urlObj.openConnection();

      long size = conn.getContentLengthLong();

      if (size > 0 && size > defaultMaxFileSize.toBytes()) {
        throw new MaxUploadSizeExceededException(defaultMaxFileSize.toBytes());
      }
    } catch (IOException | URISyntaxException e) {
      throw new ServiceException("Invalid URL: " + url, e);
    }
  }

  /**
   * Gets default max file size.
   *
   * @return the default max file size
   */
  public DataSize getDefaultMaxFileSize() {
    return defaultMaxFileSize;
  }

  /**
   * Sets default max file size.
   *
   * @param defaultMaxFileSize the default max file size
   */
  public void setDefaultMaxFileSize(DataSize defaultMaxFileSize) {
    this.defaultMaxFileSize = defaultMaxFileSize;
  }
}
