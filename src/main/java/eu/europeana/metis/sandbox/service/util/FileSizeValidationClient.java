package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.network.AbstractHttpClient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * The type File size validation client.
 */
@Service
public class FileSizeValidationClient extends AbstractHttpClient<URI, InputStream> {

  private static final int MAX_NUMBER_OF_REDIRECTS = 5;
  private static final int DEFAULT_CONNECT_TIMEOUT = 10_000;
  private static final int DEFAULT_RESPONSE_TIMEOUT = 20_000;
  private static final int DEFAULT_REQUEST_TIMEOUT = 60_000;
  private static final int DEFAULT_BUFFER_SIZE = 8 * 1024; // 8 KB
  /**
   * The Default max file size.
   */
  @Value("${spring.servlet.multipart.max-file-size}")
  @Setter
  @Getter
  private DataSize defaultMaxFileSize;

  /**
   * Instantiates a new File size validation client.
   */
  public FileSizeValidationClient() {
    this(MAX_NUMBER_OF_REDIRECTS, DEFAULT_CONNECT_TIMEOUT, DEFAULT_RESPONSE_TIMEOUT, DEFAULT_REQUEST_TIMEOUT);
  }

  /**
   * Instantiates a new File size validation client.
   *
   * @param maxRedirectCount the max redirect count
   * @param connectTimeout the connect timeout
   * @param responseTimeout the response timeout
   * @param requestTimeout the request timeout
   */
  protected FileSizeValidationClient(int maxRedirectCount, int connectTimeout, int responseTimeout, int requestTimeout) {
    super(maxRedirectCount, connectTimeout, responseTimeout, requestTimeout);
  }

  /**
   * @param providedURI provided URI
   * @return url string
   */
  @Override
  protected String getResourceUrl(URI providedURI) {
    try {
      return providedURI.toURL().toString();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * @param providedURI provided URI
   * @param actualURI actual URI
   * @param contentDisposition content disposition
   * @param mimetype file mimetype
   * @param fileSize file size
   * @param contentRetriever content retriever
   * @return InputStream
   * @throws IOException or MaxUploadSizeExceededException if the upload size is exceeded
   */
  @Override
  protected InputStream createResult(URI providedURI, URI actualURI,
      ContentDisposition contentDisposition, String mimetype, Long fileSize,
      ContentRetriever contentRetriever) throws IOException {
    if (fileSize != null && fileSize > defaultMaxFileSize.toBytes()) {
      throw new MaxUploadSizeExceededException(defaultMaxFileSize.toBytes());
    }

    int numberBytesRead;
    long totalBytesRead = 0;
    byte[] data = new byte[DEFAULT_BUFFER_SIZE];
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    while ((numberBytesRead = contentRetriever.getContent().read(data, 0, data.length)) != -1
        && totalBytesRead <= defaultMaxFileSize.toBytes()) {
      buffer.write(data, 0, numberBytesRead);
      totalBytesRead += numberBytesRead;
      if (totalBytesRead > defaultMaxFileSize.toBytes()) {
        throw new MaxUploadSizeExceededException(defaultMaxFileSize.toBytes());
      }
    }
    buffer.flush();

    return new ByteArrayInputStream(buffer.toByteArray());
  }
}
