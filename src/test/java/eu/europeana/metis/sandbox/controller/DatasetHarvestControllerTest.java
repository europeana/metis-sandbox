package eu.europeana.metis.sandbox.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static eu.europeana.metis.sandbox.common.locale.Country.GREECE;
import static eu.europeana.metis.sandbox.common.locale.Language.EL;
import static eu.europeana.metis.security.AuthenticationUtils.getUserId;
import static eu.europeana.metis.security.test.JwtUtils.BEARER;
import static eu.europeana.metis.security.test.JwtUtils.MOCK_VALID_TOKEN;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.config.SecurityConfig;
import eu.europeana.metis.sandbox.config.webmvc.WebMvcConfig;
import eu.europeana.metis.sandbox.controller.advice.RestResponseExceptionHandler;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionService;
import eu.europeana.metis.security.test.JwtUtils;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.validator.routines.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@WireMockTest
@WebMvcTest(DatasetHarvestController.class)
@ContextConfiguration(classes = {WebMvcConfig.class, RestResponseExceptionHandler.class, SecurityConfig.class,
    DatasetHarvestController.class, DatasetHarvestControllerTest.TestUrlValidatorConfig.class})
class DatasetHarvestControllerTest {

  @TestConfiguration
  static class TestUrlValidatorConfig {

    @Bean
    public UrlValidator urlValidator() {
      // Accept localhost URLs during testing
      return new UrlValidator(new String[]{"http", "https"}, UrlValidator.ALLOW_LOCAL_URLS);
    }
  }

  private static final String DATASET_NAME = "my-data-set";
  private static final String OAI_ENDPOINT_URL = "https://example-oai-endpoint.org/repository/oai";
  private static final String SETSPEC = "oai_integration_test";
  private static final String METADATA_FORMAT = "edm";
  private static final String DATASET_ID = "datasetId";
  private static final int STEP_SIZE = 1;
  private static final String XSLT_FILE_PARAM = "xsltFile";
  private static final String DATASET_FILE_PARAM = "dataset";
  private static final String ZIP_DIR = "zip";
  private static final String DATASET_VALID_ZIP = "dataset-valid.zip";
  private static final String DATASET_VALID_TAR_GZ = "sandbox.tar.gz";
  private static final String DATASET_VALID_TAR = "records_to_test.tar";
  private static String baseUrl;

  @MockitoBean
  private RateLimitInterceptor rateLimitInterceptor;

  @MockitoBean
  private DatasetExecutionService datasetExecutionService;

  @MockitoBean
  JwtDecoder jwtDecoder;

  @Autowired
  private MockMvc mockMvc;

  private final JwtUtils jwtUtils = new JwtUtils(List.of());

  @BeforeEach
  void setup(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
    baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
    stubResourceFile(wireMockRuntimeInfo, DATASET_VALID_ZIP, "application/zip");
    stubResourceFile(wireMockRuntimeInfo, DATASET_VALID_TAR_GZ, "application/gzip");
    stubResourceFile(wireMockRuntimeInfo, DATASET_VALID_TAR, "application/x-tar");
    Mockito.reset(datasetExecutionService);
  }

  private void stubResourceFile(WireMockRuntimeInfo wireMockRuntimeInfo, String fileName, String contentType) throws IOException {
    String path = ZIP_DIR + "/" + fileName;
    try (InputStream stream = DatasetHarvestControllerTest.class.getClassLoader().getResourceAsStream(path)) {
      if (stream == null) {
        throw new IllegalStateException("Test resource not found: " + path);
      }

      wireMockRuntimeInfo.getWireMock().register(get(urlEqualTo("/" + fileName))
          .willReturn(aResponse()
              .withHeader("Content-Type", contentType)
              .withBody(stream.readAllBytes())));
    }
  }

  private Jwt setupJwt() {
    Jwt jwt = jwtUtils.getEmptyRoleJwt();
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwt);
    return jwt;
  }

  private MultiValueMap<String, String> getCommonLocaleParams() {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("country", GREECE.xmlValue());
    params.add("language", EL.xmlValue());
    return params;
  }

  private HttpHeaders getCommonAuthorizationUserAgentHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, BEARER + MOCK_VALID_TOKEN);
    headers.add(HttpHeaders.USER_AGENT, "Mozilla");
    return headers;
  }

  @Test
  void harvestOaiPmh_withoutXslt_shouldSucceed() throws Exception {
    Jwt jwt = setupJwt();
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(DATASET_NAME, GREECE, EL);
    when(datasetExecutionService.createDatasetAndSubmitExecutionOai(
        datasetMetadataRequest, STEP_SIZE, OAI_ENDPOINT_URL, SETSPEC, METADATA_FORMAT, null, getUserId(jwt)
    )).thenReturn(DATASET_ID);

    mockMvc.perform(post("/dataset/{name}/harvestOaiPmh", DATASET_NAME)
               .headers(getCommonAuthorizationUserAgentHeaders())
               .params(getCommonLocaleParams())
               .param("url", OAI_ENDPOINT_URL)
               .param("setspec", SETSPEC)
               .param("metadataformat", METADATA_FORMAT))
           .andExpect(status().isAccepted())
           .andExpect(jsonPath("$.dataset-id", is(DATASET_ID)));
  }

  @Test
  void harvestOaiPmh_emptySetSpec_shouldSucceed() throws Exception {
    Jwt jwt = setupJwt();
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(DATASET_NAME, GREECE, EL);
    when(datasetExecutionService.createDatasetAndSubmitExecutionOai(
        datasetMetadataRequest, STEP_SIZE, OAI_ENDPOINT_URL, "", METADATA_FORMAT, null, getUserId(jwt)
    )).thenReturn(DATASET_ID);

    mockMvc.perform(post("/dataset/{name}/harvestOaiPmh", DATASET_NAME)
               .headers(getCommonAuthorizationUserAgentHeaders())
               .params(getCommonLocaleParams())
               .param("url", OAI_ENDPOINT_URL)
               .param("setspec", "")
               .param("metadataformat", METADATA_FORMAT))
           .andExpect(status().isAccepted())
           .andExpect(jsonPath("$.dataset-id", is(DATASET_ID)));
  }

  @Test
  void harvestOaiPmh_withXsltFile_shouldSucceed() throws Exception {
    MockMultipartFile xslt = new MockMultipartFile(XSLT_FILE_PARAM, "xslt.xsl", "application/xslt+xml", "string".getBytes());
    Jwt jwt = setupJwt();
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(DATASET_NAME, GREECE, EL);
    when(datasetExecutionService.createDatasetAndSubmitExecutionOai(
        datasetMetadataRequest, STEP_SIZE, OAI_ENDPOINT_URL, SETSPEC, METADATA_FORMAT, xslt, getUserId(jwt)
    )).thenReturn(DATASET_ID);

    mockMvc.perform(multipart("/dataset/{name}/harvestOaiPmh", DATASET_NAME)
               .file(xslt)
               .headers(getCommonAuthorizationUserAgentHeaders())
               .params(getCommonLocaleParams())
               .param("url", OAI_ENDPOINT_URL)
               .param("setspec", SETSPEC)
               .param("metadataformat", METADATA_FORMAT))
           .andExpect(status().isAccepted())
           .andExpect(jsonPath("$.dataset-id", is(DATASET_ID)));
  }

  @Test
  void harvestOaiPmh_withXsltFile_nonBrowser_shouldSucceed() throws Exception {
    MockMultipartFile xslt = new MockMultipartFile(XSLT_FILE_PARAM, "xslt.xsl", "application/xslt+xml", "string".getBytes());
    setupJwt();
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(DATASET_NAME, GREECE, EL);
    when(datasetExecutionService.createDatasetAndSubmitExecutionOai(
        datasetMetadataRequest, STEP_SIZE, OAI_ENDPOINT_URL, SETSPEC, METADATA_FORMAT, xslt, null
    )).thenReturn(DATASET_ID);

    mockMvc.perform(multipart("/dataset/{name}/harvestOaiPmh", DATASET_NAME)
               .file(xslt)
               .header("User-Agent", "non-browser")
               .params(getCommonLocaleParams())
               .param("url", OAI_ENDPOINT_URL)
               .param("setspec", SETSPEC)
               .param("metadataformat", METADATA_FORMAT))
           .andExpect(status().isAccepted())
           .andExpect(jsonPath("$.dataset-id", is(DATASET_ID)));
  }

  @Test
  void harvestOaiPmh_withoutToken_shouldFailWith401() throws Exception {
    mockMvc.perform(post("/dataset/{name}/harvestOaiPmh", DATASET_NAME)
               .header("User-Agent", "Mozilla")
               .params(getCommonLocaleParams())
               .param("url", OAI_ENDPOINT_URL)
               .param("setspec", SETSPEC)
               .param("metadataformat", METADATA_FORMAT))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void harvestOaiPmh_invalidName_expectFail() throws Exception {
    setupJwt();
    mockMvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data=set")
               .headers(getCommonAuthorizationUserAgentHeaders())
               .params(getCommonLocaleParams())
               .param("url", OAI_ENDPOINT_URL)
               .param("setspec", SETSPEC)
               .param("metadataformat", METADATA_FORMAT))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message",
               is("dataset name can only include letters, numbers, _ or - characters")));
  }

  @Test
  void harvestOaiPmh_invalidUrl_expectFail() throws Exception {
    setupJwt();
    mockMvc.perform(post("/dataset/{name}/harvestOaiPmh", DATASET_NAME)
               .headers(getCommonAuthorizationUserAgentHeaders())
               .params(getCommonLocaleParams())
               .param("url", "not a url")
               .param("setspec", SETSPEC)
               .param("metadataformat", METADATA_FORMAT))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message",
               is("The provided url is invalid. Please provide a valid url.")));
  }

  @Test
  void harvestOaiPmh_invalidStepSize_expectFail() throws Exception {
    setupJwt();
    mockMvc.perform(post("/dataset/{name}/harvestOaiPmh", DATASET_NAME)
               .headers(getCommonAuthorizationUserAgentHeaders())
               .params(getCommonLocaleParams())
               .param("url", OAI_ENDPOINT_URL)
               .param("setspec", SETSPEC)
               .param("metadataformat", METADATA_FORMAT)
               .param("stepsize", "-1"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message",
               is("Step size must be a number higher than zero")));
  }

  @Test
  void harvestOaiPmh_createDatasetAndSubmitExecutionFails_expectFail() throws Exception {
    Jwt jwt = setupJwt();
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(DATASET_NAME, GREECE, EL);
    when(datasetExecutionService.createDatasetAndSubmitExecutionOai(
        datasetMetadataRequest, STEP_SIZE, OAI_ENDPOINT_URL, SETSPEC, METADATA_FORMAT, null, getUserId(jwt)
    )).thenThrow(new IOException());

    mockMvc.perform(post("/dataset/{name}/harvestOaiPmh", DATASET_NAME)
               .headers(getCommonAuthorizationUserAgentHeaders())
               .params(getCommonLocaleParams())
               .param("url", OAI_ENDPOINT_URL)
               .param("setspec", SETSPEC)
               .param("metadataformat", METADATA_FORMAT))
           .andExpect(status().isBadRequest())
           .andExpect(result -> assertInstanceOf(IOException.class, result.getResolvedException()));
  }

  //HARVEST FILE
  @ParameterizedTest
  @MethodSource("provideDifferentCompressedFiles")
  void harvestDatasetFromFile_withoutXsltFile_expectSuccess(MockMultipartFile mockMultipart,
      CompressedFileExtension expectedExtension) throws Exception {
    Jwt jwt = setupJwt();
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(DATASET_NAME, GREECE, EL);
    when(datasetExecutionService.createDatasetAndSubmitExecutionFile(datasetMetadataRequest,
        STEP_SIZE, mockMultipart, null, getUserId(jwt), expectedExtension)
    ).thenReturn(DATASET_ID);

    mockMvc.perform(multipart("/dataset/{name}/harvestByFile", DATASET_NAME)
               .file(mockMultipart)
               .headers(getCommonAuthorizationUserAgentHeaders())
               .params(getCommonLocaleParams()))
           .andExpect(status().isAccepted())
           .andExpect(jsonPath("$.dataset-id", is(DATASET_ID)));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentCompressedFiles")
  void harvestDatasetFromFile_withXsltFile_expectSuccess(MockMultipartFile mockMultipart,
      CompressedFileExtension expectedExtension) throws Exception {
    MockMultipartFile xsltMock = new MockMultipartFile(XSLT_FILE_PARAM, "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());

    Jwt jwt = setupJwt();
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(DATASET_NAME, GREECE, EL);
    when(datasetExecutionService.createDatasetAndSubmitExecutionFile(datasetMetadataRequest,
        STEP_SIZE, mockMultipart, xsltMock, getUserId(jwt), expectedExtension)
    ).thenReturn(DATASET_ID);

    mockMvc.perform(multipart("/dataset/{name}/harvestByFile", DATASET_NAME)
               .file(mockMultipart)
               .file(xsltMock)
               .headers(getCommonAuthorizationUserAgentHeaders())
               .params(getCommonLocaleParams()))
           .andExpect(status().isAccepted())
           .andExpect(jsonPath("$.dataset-id", is(DATASET_ID)));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentCompressedFiles")
  void harvestDatasetFromFile_withXsltFile_NonBrowser_Allowed(MockMultipartFile mockMultipart,
      CompressedFileExtension expectedExtension) throws Exception {
    MockMultipartFile xsltMock = new MockMultipartFile(XSLT_FILE_PARAM, "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());

    setupJwt();
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(DATASET_NAME, GREECE, EL);
    when(datasetExecutionService.createDatasetAndSubmitExecutionFile(datasetMetadataRequest,
        STEP_SIZE, mockMultipart, xsltMock, null, expectedExtension)
    ).thenReturn(DATASET_ID);

    mockMvc.perform(multipart("/dataset/{name}/harvestByFile", DATASET_NAME)
               .file(mockMultipart)
               .file(xsltMock)
               .header("User-Agent", "non-browser")
               .params(getCommonLocaleParams()))
           .andExpect(status().isAccepted())
           .andExpect(jsonPath("$.dataset-id", is(DATASET_ID)));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentCompressedFiles")
  void harvestDatasetFromFile_Unauthenticated(MockMultipartFile mockMultipart)
      throws Exception {
    mockMvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
               .file(mockMultipart)
               .header("User-Agent", "Mozilla")
               .params(getCommonLocaleParams()))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void harvestDatasetFromFile_invalidName_expectFail() throws Exception {
    setupJwt();
    var dataset = new MockMultipartFile(DATASET_FILE_PARAM, "dataset." + ZIP_DIR, "application/" + ZIP_DIR,
        "<test></test>".getBytes());

    mockMvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data=set")
               .file(dataset)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .params(getCommonLocaleParams()))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message",
               is("dataset name can only include letters, numbers, _ or - characters")));
  }

  @Test
  void harvestDatasetFromFile_invalidStepSize_expectFail() throws Exception {
    setupJwt();
    var dataset = new MockMultipartFile(DATASET_FILE_PARAM, "dataset." + ZIP_DIR, "application/" + ZIP_DIR,
        "<test></test>".getBytes());

    mockMvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
               .file(dataset)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .params(getCommonLocaleParams())
               .param("stepsize", "-1"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message",
               is("Step size must be a number higher than zero")));
  }

  @Test
  void harvestDatasetFromFile_invalidFileType_expectFail() throws Exception {
    setupJwt();
    var dataset = new MockMultipartFile(DATASET_FILE_PARAM, "dataset.txt", "text/plain",
        "<test></test>".getBytes());

    mockMvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
               .file(dataset)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .params(getCommonLocaleParams()))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message",
               is("File provided is not valid compressed file.")));
  }

  private static Stream<Arguments> provideDifferentCompressedFiles() {
    return Stream.of(
        Arguments.of(
            new MockMultipartFile(DATASET_FILE_PARAM, "dataset." + ZIP_DIR, "application/" + ZIP_DIR, "<test></test>".getBytes()),
            CompressedFileExtension.ZIP
        ),
        Arguments.of(
            new MockMultipartFile(DATASET_FILE_PARAM, "dataset.tar", "application/x-tar", "<test></test>".getBytes()),
            CompressedFileExtension.TAR
        ),
        Arguments.of(
            new MockMultipartFile(DATASET_FILE_PARAM, "dataset.gz", "application/g" + ZIP_DIR, "<test></test>".getBytes()),
            CompressedFileExtension.GZIP
        )
    );
  }

  //HARVEST URL
  @ParameterizedTest
  @MethodSource("provideDifferentUrlsOfCompressedFiles")
  void harvestDatasetFromURL_withoutXsltFile_expectSuccess(String url, CompressedFileExtension expectedExtension)
      throws Exception {
    Jwt jwt = setupJwt();
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(DATASET_NAME, GREECE, EL);
    when(datasetExecutionService.createDatasetAndSubmitExecutionHttp(datasetMetadataRequest,
        STEP_SIZE, url, null, getUserId(jwt), expectedExtension)
    ).thenReturn(DATASET_ID);

    mockMvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
               .headers(getCommonAuthorizationUserAgentHeaders())
               .params(getCommonLocaleParams())
               .param("url", url))
           .andExpect(status().isAccepted())
           .andExpect(jsonPath("$.dataset-id", is(DATASET_ID)));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentUrlsOfCompressedFiles")
  void harvestDatasetFromURL_withXsltFile_expectSuccess(String url, CompressedFileExtension expectedExtension) throws Exception {
    MockMultipartFile xsltMock = new MockMultipartFile(XSLT_FILE_PARAM, "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());
    Jwt jwt = setupJwt();
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(DATASET_NAME, GREECE, EL);
    when(datasetExecutionService.createDatasetAndSubmitExecutionHttp(datasetMetadataRequest,
        STEP_SIZE, url, xsltMock, getUserId(jwt), expectedExtension)
    ).thenReturn(DATASET_ID);

    mockMvc.perform(multipart("/dataset/{name}/harvestByUrl", "my-data-set")
               .file(xsltMock)
               .headers(getCommonAuthorizationUserAgentHeaders())
               .params(getCommonLocaleParams())
               .param("url", url))
           .andExpect(status().isAccepted())
           .andExpect(jsonPath("$.dataset-id", is(DATASET_ID)));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentUrlsOfCompressedFiles")
  void harvestDatasetFromURL_withXsltFile_NonBrowser_Allowed(String url, CompressedFileExtension expectedExtension)
      throws Exception {
    MockMultipartFile xsltMock = new MockMultipartFile(XSLT_FILE_PARAM, "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());
    setupJwt();
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(DATASET_NAME, GREECE, EL);
    when(datasetExecutionService.createDatasetAndSubmitExecutionHttp(datasetMetadataRequest,
        STEP_SIZE, url, xsltMock, null, expectedExtension)
    ).thenReturn(DATASET_ID);

    mockMvc.perform(multipart("/dataset/{name}/harvestByUrl", "my-data-set")
               .file(xsltMock)
               .header("User-Agent", "non-browser")
               .params(getCommonLocaleParams())
               .param("url", url))
           .andExpect(status().isAccepted())
           .andExpect(jsonPath("$.dataset-id", is(DATASET_ID)));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentUrlsOfCompressedFiles")
  void harvestDatasetFromURL_Unauthenticated(String url) throws Exception {
    mockMvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
               .header("User-Agent", "Mozilla")
               .params(getCommonLocaleParams())
               .param("url", url))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void harvestDatasetFromURL_invalidName_expectFail() throws Exception {
    setupJwt();
    final String url = baseUrl + "/" + DATASET_VALID_ZIP;

    mockMvc.perform(post("/dataset/{name}/harvestByUrl", "my-data=set")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .params(getCommonLocaleParams())
               .param("url", url))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message",
               is("dataset name can only include letters, numbers, _ or - characters")));
  }

  @Test
  void harvestDatasetFromURL_invalidStepSize_expectFail() throws Exception {
    setupJwt();
    final String url = baseUrl + "/" + DATASET_VALID_ZIP;

    mockMvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .params(getCommonLocaleParams())
               .param("url", url)
               .param("stepsize", "-1"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message",
               is("Step size must be a number higher than zero")));
  }

  @Test
  void harvestDatasetFromURL_invalidFileType_expectFail() throws Exception {
    setupJwt();
    final String url = baseUrl + "/dataset-valid.INVALID";

    mockMvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .params(getCommonLocaleParams())
               .param("url", url))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message",
               is("File provided is not valid compressed file.")));
  }

  private static Stream<Arguments> provideDifferentUrlsOfCompressedFiles() {
    return Stream.of(
        Arguments.of(baseUrl + "/" + DATASET_VALID_ZIP, CompressedFileExtension.ZIP),
        Arguments.of(baseUrl + "/" + DATASET_VALID_TAR_GZ, CompressedFileExtension.GZIP),
        Arguments.of(baseUrl + "/" + DATASET_VALID_TAR, CompressedFileExtension.TAR)
    );
  }
}
