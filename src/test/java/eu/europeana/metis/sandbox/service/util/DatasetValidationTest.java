package eu.europeana.metis.sandbox.service.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.europeana.metis.sandbox.common.exception.DatasetFileSizeException;
import org.junit.jupiter.api.Test;

/**
 * The type Dataset validation test.
 */
@WireMockTest
class DatasetValidationTest {

  /**
   * Check url content length.
   *
   * @param wireMockRuntimeInfo the wire mock runtime info
   */
  @Test
  void checkUrlContentLength(WireMockRuntimeInfo wireMockRuntimeInfo) {
    stubFor(get("/largefile").willReturn(aResponse()
        .withHeader("Content-Type", "application/octet-stream")
        .withHeader("Content-Length", String.valueOf(DatasetValidation.DEFAULT_MAX_FILE_SIZE + 1))
        .withBody("large content")));

    String testUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/largefile";
    DatasetFileSizeException exception = assertThrows(DatasetFileSizeException.class,
        () -> DatasetValidation.checkUrlContentLength(testUrl));
    assertEquals("File content too large: 67108865 bytes, max allowed: 67108864 bytes",
        exception.getMessage());
  }

  /**
   * Check url content length invalid url.
   */
  @Test
  void checkUrlContentLength_InvalidURL() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, ()->
    DatasetValidation.checkUrlContentLength("my url"));
    assertEquals("Invalid URL: my url", exception.getMessage());
  }
}
