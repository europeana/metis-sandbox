package eu.europeana.metis.sandbox.service.util;

import static org.apache.commons.lang3.math.NumberUtils.isParsable;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * The type Dataset validation.
 */
@Service
public final class DatasetValidationService {

  private static final int DEFAULT_CONNECT_TIMEOUT = 10000;
  private static final int DEFAULT_SOCKET_TIMEOUT = 50000;
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
      new URI(url);

      long size = getFromUrlContentLengthOfFile(url);

      if (size > 0 && size > defaultMaxFileSize.toBytes()) {
        throw new MaxUploadSizeExceededException(defaultMaxFileSize.toBytes());
      }
    } catch (URISyntaxException | FileNotFoundException e) {
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

  /**
   * Gets from url content length of file.
   *
   * @param urlString the url string
   * @return the from url content length of file
   */
  private long getFromUrlContentLengthOfFile(String urlString) throws FileNotFoundException {
    try {
      new URI(urlString);
    } catch (URISyntaxException e) {
      throw new ServiceException("Invalid URL: " + urlString, e);
    }
    int statusCode;
    long fileSize = -1L;
    try (CloseableHttpClient client =
        HttpClientBuilder.create()
                         .setDefaultRequestConfig(
                             RequestConfig.custom()
                                          .setRedirectsEnabled(true)
                                          .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
                                          .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT)
                                          .build())
                         .build()) {

      HttpHead head = new HttpHead(urlString);

      HttpResponse response = client.execute(head);
      StatusLine statusLine = response.getStatusLine();
      statusCode = statusLine.getStatusCode();
      if (statusCode == HttpStatus.NOT_FOUND.value()) {
        throw new FileNotFoundException("File does not exist or cannot be accessed.");
      } else if (statusCode < HttpStatus.OK.value() || statusCode >= HttpStatus.MULTIPLE_CHOICES.value()) {
        throw new ServiceException("Failed to retrieve file size. HTTP Status Code: " + statusCode);
      }

      if (response.getEntity() != null &&
          response.getEntity().getContentLength() >= 0) {
        fileSize = response.getEntity().getContentLength();
      } else if (response.getFirstHeader(CONTENT_LENGTH) != null
          && isParsable(response.getFirstHeader(CONTENT_LENGTH).getValue())) {
        fileSize = Long.parseLong(response.getFirstHeader(CONTENT_LENGTH).getValue());
      }

    } catch (IOException e) {
      throw new ServiceException("Error while checking file size for URL: " + urlString, e);
    }
    return fileSize;
  }
}
