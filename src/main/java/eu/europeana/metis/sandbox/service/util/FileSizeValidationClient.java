package eu.europeana.metis.sandbox.service.util;

import static org.apache.commons.lang3.math.NumberUtils.isParsable;
import static org.apache.tika.metadata.TikaCoreProperties.RESOURCE_NAME_KEY;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;

import eu.europeana.metis.network.AbstractHttpClient;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.utils.TempFileUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.tika.metadata.Metadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
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
  @Value("${spring.servlet.multipart.max-file-size}")
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
   * @throws IOException
   */
  @Override
  protected InputStream createResult(URI providedURI, URI actualURI, ContentDisposition contentDisposition, String mimetype, Long fileSize,
      ContentRetriever contentRetriever) throws IOException {
    if (fileSize != null && fileSize > defaultMaxFileSize.toBytes()) {
      throw new MaxUploadSizeExceededException(defaultMaxFileSize.toBytes());
    }
    Path tempFile = TempFileUtils.createSecureTempFile("filesize-validation", ".tmp");
    Files.copy(contentRetriever.getContent(), tempFile, StandardCopyOption.REPLACE_EXISTING);
    return new BoundedInputStream(Files.newInputStream(tempFile), defaultMaxFileSize.toBytes());
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
