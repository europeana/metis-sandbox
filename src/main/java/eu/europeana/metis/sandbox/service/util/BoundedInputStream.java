package eu.europeana.metis.sandbox.service.util;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * The type Bounded input stream.
 */
public class BoundedInputStream extends InputStream {

  private final InputStream delegate;
  private final long maxBytes;
  private long byteCount;

  /**
   * Instantiates a new Bounded input stream.
   *
   * @param delegate the delegate
   * @param maxBytes the max bytes
   */
  public BoundedInputStream(InputStream delegate, long maxBytes) {
    this.delegate = delegate;
    this.maxBytes = maxBytes;
  }

  @Override
  public int read() throws IOException {
    int bytesRead = delegate.read();
    ++byteCount;
    if (bytesRead != -1 && byteCount > maxBytes) {
      throw new MaxUploadSizeExceededException(maxBytes);
    }
    return bytesRead;
  }

  @Override
  public int read(byte[] byteArray, int off, int len) throws IOException {
    int bytesRead = delegate.read(byteArray, off, len);
    if (bytesRead > 0) {
      this.byteCount += bytesRead;
      if (this.byteCount > maxBytes) {
        throw new MaxUploadSizeExceededException(maxBytes);
      }
    }
    return bytesRead;
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }
}
