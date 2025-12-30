package eu.europeana.metis.sandbox.service.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class BoundedInputStreamTest {

  private BoundedInputStream boundedInputStream;

  @Test
  void read_byte() {
    boundedInputStream = new BoundedInputStream(new ByteArrayInputStream(new byte[64]), 64);
    assertDoesNotThrow(() -> {
      int bRead;
      do {
        bRead = boundedInputStream.read();
      } while (bRead != -1);
    });
  }

  @Test
  void read_byte_exception() {
    boundedInputStream = new BoundedInputStream(new ByteArrayInputStream(new byte[64]), 48);
    assertThrows(MaxUploadSizeExceededException.class, () -> {
          for (int i = 0; i < 64; i++) {
            boundedInputStream.read();
          }
        }
    );
  }

  @Test
  void read_array() {
    boundedInputStream = new BoundedInputStream(new ByteArrayInputStream(new byte[64]), 64);
    assertDoesNotThrow(() -> {
      byte[] buffer = new byte[16];
      int bRead;
      do {
        bRead = boundedInputStream.read(buffer, 0, buffer.length);
      } while (bRead != -1);
    });
  }

  @Test
  void read_array_exception() {
    boundedInputStream = new BoundedInputStream(new ByteArrayInputStream(new byte[64]), 48);
    assertThrows(MaxUploadSizeExceededException.class, () -> {
      byte[] buffer = new byte[16];
      for (int i = 0; i < 4; i++) {
        boundedInputStream.read(buffer, 0, buffer.length);
      }
    });
  }
}
