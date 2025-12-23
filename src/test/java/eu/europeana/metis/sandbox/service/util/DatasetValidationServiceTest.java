package eu.europeana.metis.sandbox.service.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getOrHead;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * The type Dataset validation test.
 */
@WireMockTest
class DatasetValidationServiceTest {

  private DatasetValidationService datasetValidationService;

  @BeforeEach
  void setUp() {
    datasetValidationService = new DatasetValidationService();
    datasetValidationService.setDefaultMaxFileSize(DataSize.of(64, DataUnit.MEGABYTES));
  }

  /**
   * Check url content length.
   *
   * @param wireMockRuntimeInfo the wire mock runtime info
   */
  @Test
  void checkUrlContentLength(WireMockRuntimeInfo wireMockRuntimeInfo) {
    stubFor(getOrHead(urlEqualTo("/largefile")).willReturn(aResponse()
        .withHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM_VALUE)
        .withHeader(CONTENT_LENGTH, String.valueOf(datasetValidationService.getDefaultMaxFileSize().toBytes() + 1))
        .withStatus(HttpStatus.OK.value())
        .withBody("large content")));

    String testUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/largefile";
    MaxUploadSizeExceededException exception = assertThrows(MaxUploadSizeExceededException.class,
        () -> datasetValidationService.checkUrlContentLength(testUrl));
    assertEquals("Maximum upload size of 67108864 bytes exceeded",
        exception.getMessage());
  }

  /**
   * Check url content length not found.
   *
   * @param wireMockRuntimeInfo the wire mock runtime info
   */
  @Test
  void checkUrlContentLength_NotFound(WireMockRuntimeInfo wireMockRuntimeInfo) {
    stubFor(getOrHead(urlEqualTo("/largefile")).willReturn(aResponse()
        .withHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM_VALUE)
        .withStatus(HttpStatus.NOT_FOUND.value())));

    String testUrl = wireMockRuntimeInfo.getHttpBaseUrl() + "/largefile";
    ServiceException exception = assertThrows(ServiceException.class,
        () -> datasetValidationService.checkUrlContentLength(testUrl));
    assertInstanceOf(FileNotFoundException.class, exception.getCause());
    assertEquals("File does not exist or cannot be accessed.",
        exception.getCause().getMessage());
  }

  /**
   * Check url content length invalid url.
   */
  @Test
  void checkUrlContentLength_InvalidURL() {
    ServiceException exception = assertThrows(ServiceException.class, () ->
        datasetValidationService.checkUrlContentLength("my url"));
    assertEquals("Invalid URL: my url", exception.getMessage());
  }
}
