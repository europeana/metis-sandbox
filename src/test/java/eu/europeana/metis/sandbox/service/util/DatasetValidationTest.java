package eu.europeana.metis.sandbox.service.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * The type Dataset validation test.
 */
@WireMockTest
class DatasetValidationTest {

  private DatasetValidationService datasetValidation;

  @BeforeEach
  void setUp() {
    datasetValidation = new DatasetValidationService();
    datasetValidation.setDefaultMaxFileSize(DataSize.of(64, DataUnit.MEGABYTES));
  }

  /**
   * Check url content length.
   *
   * @param wireMockRuntimeInfo the wire mock runtime info
   */
  @Test
  void checkUrlContentLength(WireMockRuntimeInfo wireMockRuntimeInfo) {
    stubFor(get("/largefile").willReturn(aResponse()
        .withHeader("Content-Type", "application/octet-stream")
        .withHeader("Content-Length", String.valueOf(datasetValidation.getDefaultMaxFileSize().toBytes() + 1))
        .withBody("large content")));

    String testUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/largefile";
    MaxUploadSizeExceededException exception = assertThrows(MaxUploadSizeExceededException.class,
        () -> datasetValidation.checkUrlContentLength(testUrl));
    assertEquals("Maximum upload size of 67108864 bytes exceeded",
        exception.getMessage());
  }

  /**
   * Check url content length invalid url.
   */
  @Test
  void checkUrlContentLength_InvalidURL() {
    ServiceException exception = assertThrows(ServiceException.class, () ->
        datasetValidation.checkUrlContentLength("my url"));
    assertEquals("Invalid URL: my url", exception.getMessage());
  }
}
