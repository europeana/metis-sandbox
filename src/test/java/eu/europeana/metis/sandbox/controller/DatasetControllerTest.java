package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static eu.europeana.metis.security.test.JwtUtils.BEARER;
import static eu.europeana.metis.security.test.JwtUtils.MOCK_VALID_TOKEN;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.indexing.tiers.view.ContentTierBreakdown;
import eu.europeana.indexing.tiers.view.RecordTierCalculationSummary;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.indexing.utils.LicenseType;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.config.SecurityConfig;
import eu.europeana.metis.sandbox.config.webmvc.WebMvcConfig;
import eu.europeana.metis.sandbox.controller.advice.RestResponseExceptionHandler;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.FileHarvestingDTO;
import eu.europeana.metis.sandbox.dto.HttpHarvestingDTO;
import eu.europeana.metis.sandbox.dto.OAIPmhHarvestingDTO;
import eu.europeana.metis.sandbox.dto.RecordTiersInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDTO;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.TierStatisticsDTO;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfoDTO;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService;
import eu.europeana.metis.security.test.JwtUtils;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(DatasetController.class)
@ContextConfiguration(classes = {WebMvcConfig.class, DatasetController.class, SecurityConfig.class, RestResponseExceptionHandler.class})
class DatasetControllerTest {

  @MockBean
  private RateLimitInterceptor rateLimitInterceptor;

  @MockBean
  private DatasetService datasetService;

  @MockBean
  private DatasetReportService datasetReportService;

  @MockBean
  private RecordTierCalculationService recordTierCalculationService;

  @MockBean
  JwtDecoder jwtDecoder;

  @Mock
  private CompletableFuture<Void> asyncResult;

  private static MockMvc mvc;
  private final JwtUtils jwtUtils;

  public DatasetControllerTest() {
    jwtUtils = new JwtUtils(List.of());
  }

  @BeforeAll
  static void setup(WebApplicationContext context) {
    mvc = MockMvcBuilders.webAppContextSetup(context)
                         .apply(SecurityMockMvcConfigurers.springSecurity())
                         .defaultRequest(get("/"))
                         .build();
  }

  @BeforeEach
  public void setup() {
    reset(jwtDecoder);
  }

  private static Stream<MultipartFile> provideDifferentCompressedFiles() {
    return Stream.of(
        new MockMultipartFile("dataset", "dataset.txt", "application/zip",
            "<test></test>".getBytes()),
        new MockMultipartFile("dataset", "dataset.txt", "application/x-tar",
            "<test></test>".getBytes()),
        new MockMultipartFile("dataset", "dataset.txt", "application/gzip",
            "<test></test>".getBytes())
    );
  }

  private static Stream<String> provideDifferentUrlsOfCompressedFiles() {
    return Stream.of(
        Paths.get("zip", "dataset-valid.zip").toUri().toString(),
        Paths.get("zip", "sandbox.tar.gz").toUri().toString(),
        Paths.get("zip", "records_to_test.tar").toUri().toString()
    );
  }

  private static Stream<Arguments> steps() {
    return Stream.of(
        arguments(null, Set.of(FullBatchJobType.HARVEST_FILE, FullBatchJobType.HARVEST_OAI), status().isOk(),
            content().string("exampleString")),
        arguments("", Set.of(FullBatchJobType.HARVEST_FILE, FullBatchJobType.HARVEST_OAI), status().isOk(),
            content().string("exampleString")),
        arguments("HARVEST", Set.of(FullBatchJobType.HARVEST_FILE, FullBatchJobType.HARVEST_OAI), status().isOk(),
            content().string("exampleString")),
        arguments("TRANSFORM_TO_EDM_EXTERNAL", Set.of(FullBatchJobType.TRANSFORM_EXTERNAL),
            status().isOk(), content().string("exampleString")),
        arguments("VALIDATE_EXTERNAL", Set.of(FullBatchJobType.VALIDATE_EXTERNAL), status().isOk(),
            content().string("exampleString")),
        arguments("TRANSFORM", Set.of(FullBatchJobType.TRANSFORM_INTERNAL), status().isOk(),
            content().string("exampleString")),
        arguments("VALIDATE_INTERNAL", Set.of(FullBatchJobType.VALIDATE_INTERNAL), status().isOk(),
            content().string("exampleString")),
        arguments("NORMALIZE", Set.of(FullBatchJobType.NORMALIZE), status().isOk(),
            content().string("exampleString")),
        arguments("ENRICH", Set.of(FullBatchJobType.ENRICH), status().isOk(),
            content().string("exampleString")),
        arguments("MEDIA_PROCESS", Set.of(FullBatchJobType.MEDIA), status().isOk(),
            content().string("exampleString")),
        arguments("PUBLISH", Set.of(FullBatchJobType.INDEX), status().isOk(),
            content().string("exampleString")),
        arguments("NON_SENSE", Set.of(), status().isBadRequest(),
            content().string("{\"statusCode\":400,\"status\":\"BAD_REQUEST\",\"message\":\"Invalid step name NON_SENSE\"}"))
    );
  }

  @ParameterizedTest
  @MethodSource("provideDifferentCompressedFiles")
  void processDatasetFromZipFile_withoutXsltFile_expectSuccess(MockMultipartFile mockMultipart) throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
           .file(mockMultipart)
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .header("User-Agent", "Mozilla")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("stepsize", "2"))
       .andExpect(status().isAccepted())
       .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentCompressedFiles")
  void processDatasetFromZipFile_withXsltFile_expectSuccess(MockMultipartFile mockMultipart) throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());

    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
           .file(mockMultipart)
           .file(xsltMock)
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .header("User-Agent", "Mozilla")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("stepsize", "2"))
       .andExpect(status().isAccepted())
       .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentCompressedFiles")
  void processDatasetFromZipFile_withXsltFile_NonBrowser_Allowed(MockMultipartFile mockMultipart) throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());

    when(datasetService.createDataset(any(), eq("my-data-set"), isNull(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
           .file(mockMultipart)
           .file(xsltMock)
           .header("User-Agent", "non-browser")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("stepsize", "2"))
       .andExpect(status().isAccepted())
       .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentCompressedFiles")
  void processDatasetFromZipFile_Unauthenticated(MockMultipartFile mockMultipart) throws Exception {
    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
           .file(mockMultipart)
           .header("User-Agent", "Mozilla")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("stepsize", "2"))
       .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @MethodSource("provideDifferentUrlsOfCompressedFiles")
  void processDatasetFromURL_withoutXsltFile_expectSuccess(String url) throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .header("User-Agent", "Mozilla")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("url", url)
           .param("stepsize", "2"))
       .andExpect(status().isAccepted())
       .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentUrlsOfCompressedFiles")
  void processDatasetFromURL_withXsltFile_expectSuccess(String url) throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());

    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(multipart("/dataset/{name}/harvestByUrl", "my-data-set")
           .file(xsltMock)
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .header("User-Agent", "Mozilla")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("url", url)
           .param("stepsize", "2"))
       .andExpect(status().isAccepted())
       .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentUrlsOfCompressedFiles")
  void processDatasetFromURL_withXsltFile_NonBrowser_Allowed(String url) throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());

    when(datasetService.createDataset(any(), eq("my-data-set"), isNull(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(multipart("/dataset/{name}/harvestByUrl", "my-data-set")
           .file(xsltMock)
           .header("User-Agent", "non-browser")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("url", url)
           .param("stepsize", "2"))
       .andExpect(status().isAccepted())
       .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @ParameterizedTest
  @MethodSource("provideDifferentUrlsOfCompressedFiles")
  void processDatasetFromURL_Unauthenticated(String url) throws Exception {
    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
           .header("User-Agent", "Mozilla")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("url", url)
           .param("stepsize", "2"))
       .andExpect(status().isUnauthorized());
  }

  @Test
  void processDatasetFromOAI_expectSuccess() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    final String url = new URI("https://metis-repository-rest.test.eanadev.org/repository/oai").toString();

    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .header("User-Agent", "Mozilla")
           .param("country", ITALY.xmlValue())
           .param("language", IT.xmlValue())
           .param("url", url)
           .param("setspec", "oai_integration_test")
           .param("metadataformat", "edm")
           .param("stepsize", "2"))
       .andExpect(status().isAccepted())
       .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @Test
  void processDatasetFromOAIWithEmptySetSpec_expectSuccess() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    final String url = new URI("https://metis-repository-rest.test.eanadev.org/repository/oai").toString();

    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .header("User-Agent", "Mozilla")
           .param("country", ITALY.xmlValue())
           .param("language", IT.xmlValue())
           .param("url", url)
           .param("setspec", "")
           .param("metadataformat", "edm")
           .param("stepsize", "2"))
       .andExpect(status().isAccepted())
       .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @Test
  void processDatasetFromOAIWithXsltFile_expectSuccess() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    final String url = new URI("https://metis-repository-rest.test.eanadev.org/repository/oai").toString();

    MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());

    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(multipart("/dataset/{name}/harvestOaiPmh", "my-data-set")
           .file(xsltMock)
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .header("User-Agent", "Mozilla")
           .param("country", ITALY.xmlValue())
           .param("language", IT.xmlValue())
           .param("url", url)
           .param("setspec", "oai_integration_test")
           .param("metadataformat", "edm")
           .param("stepsize", "2"))
       .andExpect(status().isAccepted())
       .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @Test
  void processDatasetFromOAIWithXsltFile_NonBrowser_Allowed() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    final String url = new URI("https://metis-repository-rest.test.eanadev.org/repository/oai").toString();

    MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());

    when(datasetService.createDataset(any(), eq("my-data-set"), isNull(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(multipart("/dataset/{name}/harvestOaiPmh", "my-data-set")
           .file(xsltMock)
           .header("User-Agent", "non-browser")
           .param("country", ITALY.xmlValue())
           .param("language", IT.xmlValue())
           .param("url", url)
           .param("setspec", "oai_integration_test")
           .param("metadataformat", "edm")
           .param("stepsize", "2"))
       .andExpect(status().isAccepted())
       .andExpect(jsonPath("$.dataset-id", is("12345")));
  }

  @Test
  void processDatasetFromOAI_Unauthenticated() throws Exception {
    final String url = new URI("https://metis-repository-rest.test.eanadev.org/repository/oai").toString();
    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
           .header("User-Agent", "Mozilla")
           .param("country", ITALY.xmlValue())
           .param("language", IT.xmlValue())
           .param("url", url)
           .param("setspec", "oai_integration_test")
           .param("metadataformat", "edm")
           .param("stepsize", "2"))
       .andExpect(status().isUnauthorized());
  }

  @Test
  void processDatasetFromFile_invalidName_expectFail() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    var dataset = new MockMultipartFile("dataset", "dataset.txt", "application/zip",
        "<test></test>".getBytes());

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data=set")
           .file(dataset)
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .param("country", ITALY.name())
           .param("language", IT.name()))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.message",
           is("dataset name can only include letters, numbers, _ or - characters")));
  }

  @Test
  void processDatasetFromFile_invalidStepSize_expectFail() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    var dataset = new MockMultipartFile("dataset", "dataset.txt", "application/zip",
        "<test></test>".getBytes());

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
           .file(dataset)
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("stepsize", "-1"))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.message",
           is("Step size must be a number higher than zero")));
  }

  @Test
  void processDatasetFromFile_invalidFileType_expectFail() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    var dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        "<test></test>".getBytes());

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
           .file(dataset)
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("stepsize", "-1"))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.message",
           is("File provided is not valid compressed file. ")));
  }

  @Test
  void processDatasetFromURL_invalidName_expectFail() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    final String url = "zip" + File.separator + "dataset-valid.zip";

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data=set")
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .param("name", "invalidDatasetName")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("url", url))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.message",
           is("dataset name can only include letters, numbers, _ or - characters")));
  }

  @Test
  void processDatasetFromURL_invalidStepSize_expectFail() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    final String url = Paths.get("zip" + File.separator + "dataset-valid.zip").toUri().toString();

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .param("name", "invalidDatasetName")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("url", url)
           .param("stepsize", "-1"))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.message",
           is("Step size must be a number higher than zero")));
  }

  @Test
  void processDatasetFromOAI_invalidName_expectFail() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    final String url = new URI("https://metis-repository-rest.test.eanadev.org/repository/oai").toString();

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data=set")
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .param("name", "invalidDatasetName")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("url", url)
           .param("setspec", "oai_integration_test")
           .param("metadataformat", "edm"))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.message",
           is("dataset name can only include letters, numbers, _ or - characters")));
  }

  @Test
  void processDatasetFromOAI_invalidStepSize_expectFail() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    final String url = new URI("https://metis-repository-rest.test.eanadev.org/repository/oai").toString();

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .param("name", "invalidDatasetName")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("url", url)
           .param("setspec", "oai_integration_test")
           .param("metadataformat", "edm")
           .param("stepsize", "-1"))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.message",
           is("Step size must be a number higher than zero")));
  }

  @Test
  void processDatasetFromOAI_harvestServiceFails_expectFail() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    final String url = new URI("https://metis-repository-rest.test.eanadev.org/repository/oai").toString();

    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("url", url)
           .param("setspec", "oai_integration_test")
           .param("metadataformat", "edm"))
       .andExpect(status().isBadRequest())
       .andExpect(result -> assertTrue(
           result.getResolvedException() instanceof IllegalArgumentException));
  }

  @Test
  void processDatasetFromOAI_datasetServiceFails_expectFail() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    final String url = new URI("https://metis-repository-rest.test.eanadev.org/repository/oai").toString();

    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenThrow(new ServiceException("Failed", new Exception()));

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .header("User-Agent", "Mozilla")
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("url", url)
           .param("setspec", "oai_integration_test")
           .param("metadataformat", "edm"))
       .andExpect(status().isInternalServerError())
       .andExpect(jsonPath("$.message",
           is("Failed Please retry, if problem persists contact provider.")));
  }


  @Test
  void processDatasetFromOAI_differentXsltFileType_expectFail() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    final String url = new URI("https://metis-repository-rest.test.eanadev.org/repository/oai").toString();

    MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl", "application/zip",
        "string".getBytes());

    mvc.perform(multipart("/dataset/{name}/harvestOaiPmh", "my-data-set")
           .file(xsltMock)
           .header("Authorization", BEARER + MOCK_VALID_TOKEN)
           .param("country", ITALY.name())
           .param("language", IT.name())
           .param("url", url)
           .param("setspec", "oai_integration_test")
           .param("metadataformat", "edm"))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.message",
           is("The given xslt file should be a single xml file.")));
  }

  @Test
  void retrieveDatasetProgress_expectSuccess() throws Exception {
    var message1 = "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.";
    var message2 = "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.";
    var error1 = new ErrorInfoDTO(message1, Status.FAIL, List.of("1", "2"));
    var error2 = new ErrorInfoDTO(message2, Status.FAIL, List.of("3", "4"));
    var errors = List.of(error1, error2);
    var createProgress = new ProgressByStepDTO(FullBatchJobType.HARVEST_FILE, 10, 0, 0, List.of());
    var externalProgress = new ProgressByStepDTO(FullBatchJobType.VALIDATE_EXTERNAL, 7, 3, 0, errors);
    var tiersZeroInfo = new TiersZeroInfoDTO(new TierStatisticsDTO(0, Collections.emptyList()),
        new TierStatisticsDTO(0, Collections.emptyList()));
    var report = new ProgressInfoDTO("https://metis-sandbox",
        10L, 10L, List.of(createProgress, externalProgress), false, "", emptyList(),
        tiersZeroInfo);
//    when(datasetReportService.getReport("1")).thenReturn(report);

    mvc.perform(get("/dataset/{id}/progress", "1"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.status",
           is("COMPLETED")))
       .andExpect(jsonPath("$.progress-by-step[1].errors[0].message",
           is(message1)))
       .andExpect(jsonPath("$.progress-by-step[1].errors[1].message",
           is(message2)));
  }

  @Test
  void retrieveDatasetProgress_datasetInvalidDatasetId_expectFail() throws Exception {

//    when(datasetReportService.getReport("1"))
//        .thenThrow(new InvalidDatasetException("1"));

    mvc.perform(get("/dataset/{id}/progress", "1"))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.message",
           is("Provided dataset id: [1] is not valid. ")));
  }

  @Test
  void retrieveDatasetProgress_datasetReportServiceFails_expectFail() throws Exception {

//    when(datasetReportService.getReport("1"))
//        .thenThrow(new ServiceException("Failed", new Exception()));

    mvc.perform(get("/dataset/{id}/progress", "1"))
       .andExpect(status().isInternalServerError())
       .andExpect(jsonPath("$.message",
           is("Failed Please retry, if problem persists contact provider.")));
  }

  @Test
  void retrieveDatasetInfo_fileHarvesting_expectSuccess() throws Exception {
    Instant minInstant = Instant.ofEpochMilli(Long.MIN_VALUE);
    ZonedDateTime mockTime = minInstant.atZone(ZoneOffset.UTC);
    DatasetInfoDTO mock = new DatasetInfoDTO.Builder()
        .datasetId("1")
        .datasetName("datasetName")
        .creationDate(mockTime)
        .language(IT)
        .country(ITALY)
        .harvestingParametricDto(new FileHarvestingDTO("fileName", "fileType"))
        .transformedToEdmExternal(false)
        .build();

    when(datasetService.getDatasetInfo("1")).thenReturn(mock);

    mvc.perform(get("/dataset/{id}/info", "1"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.dataset-id", is("1")))
       .andExpect(jsonPath("$.dataset-name", is("datasetName")))
       .andExpect(jsonPath("$.creation-date", is("-292275055-05-16T16:47:04.192Z")))
       .andExpect(jsonPath("$.language", is("Italian")))
       .andExpect(jsonPath("$.country", is("Italy")))
       .andExpect(jsonPath("$.transformed-to-edm-external", is(false)))
       .andExpect(jsonPath("$.harvesting-parameters.file-name", is("fileName")))
       .andExpect(jsonPath("$.harvesting-parameters.file-type", is("fileType")))
       .andExpect(jsonPath("$.harvesting-parameters.url").doesNotExist())
       .andExpect(jsonPath("$.harvesting-parameters.set-spec").doesNotExist())
       .andExpect(jsonPath("$.harvesting-parameters.metadata-format").doesNotExist());

  }

  @Test
  void retrieveDatasetInfo_httpHarvesting_expectSuccess() throws Exception {
    Instant minInstant = Instant.ofEpochMilli(Long.MIN_VALUE);
    ZonedDateTime mockTime = minInstant.atZone(ZoneOffset.UTC);
    DatasetInfoDTO mock = new DatasetInfoDTO.Builder()
        .datasetId("1")
        .datasetName("datasetName")
        .creationDate(mockTime)
        .language(IT)
        .country(ITALY)
        .harvestingParametricDto(new HttpHarvestingDTO("http://url-to-test.com"))
        .transformedToEdmExternal(false)
        .build();

    when(datasetService.getDatasetInfo("1")).thenReturn(mock);

    mvc.perform(get("/dataset/{id}/info", "1"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.dataset-id", is("1")))
       .andExpect(jsonPath("$.dataset-name", is("datasetName")))
       .andExpect(jsonPath("$.creation-date", is("-292275055-05-16T16:47:04.192Z")))
       .andExpect(jsonPath("$.language", is("Italian")))
       .andExpect(jsonPath("$.country", is("Italy")))
       .andExpect(jsonPath("$.transformed-to-edm-external", is(false)))
       .andExpect(jsonPath("$.harvesting-parameters.url", is("http://url-to-test.com")))
       .andExpect(jsonPath("$.harvesting-parameters.file-name").doesNotExist())
       .andExpect(jsonPath("$.harvesting-parameters.file-type").doesNotExist())
       .andExpect(jsonPath("$.harvesting-parameters.set-spec").doesNotExist())
       .andExpect(jsonPath("$.harvesting-parameters.metadata-format").doesNotExist());

  }

  @Test
  void retrieveDatasetInfo_oaiPmhHarvesting_expectSuccess() throws Exception {
    Instant minInstant = Instant.ofEpochMilli(Long.MIN_VALUE);
    ZonedDateTime mockTime = minInstant.atZone(ZoneOffset.UTC);
    DatasetInfoDTO mock = new DatasetInfoDTO.Builder()
        .datasetId("1")
        .datasetName("datasetName")
        .creationDate(mockTime)
        .language(IT)
        .country(ITALY)
        .harvestingParametricDto(new OAIPmhHarvestingDTO("http://url-to-test.com", "setSpec", "metadataFormat"))
        .transformedToEdmExternal(false)
        .build();

    when(datasetService.getDatasetInfo("1")).thenReturn(mock);

    mvc.perform(get("/dataset/{id}/info", "1"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.dataset-id", is("1")))
       .andExpect(jsonPath("$.dataset-name", is("datasetName")))
       .andExpect(jsonPath("$.creation-date", is("-292275055-05-16T16:47:04.192Z")))
       .andExpect(jsonPath("$.language", is("Italian")))
       .andExpect(jsonPath("$.country", is("Italy")))
       .andExpect(jsonPath("$.transformed-to-edm-external", is(false)))
       .andExpect(jsonPath("$.harvesting-parameters.url", is("http://url-to-test.com")))
       .andExpect(jsonPath("$.harvesting-parameters.set-spec", is("setSpec")))
       .andExpect(jsonPath("$.harvesting-parameters.metadata-format", is("metadataFormat")))
       .andExpect(jsonPath("$.harvesting-parameters.file-name").doesNotExist())
       .andExpect(jsonPath("$.harvesting-parameters.file-type").doesNotExist());

  }

  @Test
  void retrieveDatasetInfo_datasetDoesNotExist_expectFail() throws Exception {
    when(datasetService.getDatasetInfo("1")).thenThrow(new InvalidDatasetException("1"));

    mvc.perform(get("/dataset/{id}/info", "1"))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.message", is("Provided dataset id: [1] is not valid. ")));

  }

  @Test
  void computeRecordTierCalculation_expectSuccess() throws Exception {
    final String datasetId = "1";
    final String recordId = "recordId";
    final String europeanaId = "europeanaId";

    final RecordTierCalculationSummary recordTierCalculationSummary = new RecordTierCalculationSummary();
    recordTierCalculationSummary.setEuropeanaRecordId(europeanaId);
    final RecordTierCalculationView recordTierCalculationView = new RecordTierCalculationView(
        recordTierCalculationSummary,
        new ContentTierBreakdown.Builder().build(), null);
    when(recordTierCalculationService.calculateTiers(recordId, datasetId)).thenReturn(
        recordTierCalculationView);

    mvc.perform(get("/dataset/{id}/record/compute-tier-calculation", datasetId)
           .param("recordId", recordId))
       .andExpect(jsonPath("$.recordTierCalculationSummary.europeanaRecordId", is("europeanaId")))
       .andExpect(jsonPath("$.recordTierCalculationSummary.contentTier", isEmptyOrNullString()));
  }

  @Test
  void computeRecordTierCalculation_NoRecordFoundException() throws Exception {
    final String datasetId = "1";
    final String recordId = "recordId";
    when(recordTierCalculationService.calculateTiers(anyString(), anyString())).thenThrow(
        new NoRecordFoundException("record not found"));
    mvc.perform(get("/dataset/{id}/record/compute-tier-calculation", datasetId)
           .param("recordId", recordId))
       .andExpect(status().isNotFound())
       .andExpect(jsonPath("$.message",
           is("record not found")));
  }

  @ParameterizedTest
  @MethodSource("steps")
  void getRecord_expectSuccess(String step, Set<FullBatchJobType> steps, ResultMatcher expectedStatus,
      ResultMatcher expectedContent) throws Exception {
    final String datasetId = "1";
    final String recordId = "europeanaId";
    final String returnString = "exampleString";
//    when(recordLogService.getProviderRecordString(recordId, datasetId, steps))
//        .thenReturn(returnString);

    mvc.perform(get("/dataset/{id}/record", datasetId)
           .param("recordId", recordId)
           .param("step", step))
       .andExpect(expectedStatus)
       .andExpect(expectedContent);
  }

  @Test
  void getRecord_stepOptional_expectSuccess() throws Exception {
    final String datasetId = "1";
    final String recordId = "europeanaId";
    final String returnString = "exampleString";
//    when(recordLogService.getProviderRecordString(recordId, datasetId,
//        Set.of(Step.HARVEST_FILE, Step.HARVEST_OAI_PMH)))
//        .thenReturn(returnString);

    mvc.perform(get("/dataset/{id}/record", datasetId)
           .param("recordId", recordId))
       .andExpect(content().string(returnString));
  }

  @Test
  void getRecord_NoRecordFoundException() throws Exception {
    final String datasetId = "1";
    final String recordId = "europeanaId";
    final String step = "HARVEST";
//    when(recordLogService.getProviderRecordString(anyString(), anyString(),
//        any(Set.class))).thenThrow(
//        new NoRecordFoundException("record not found"));

    mvc.perform(get("/dataset/{id}/record", datasetId)
           .param("recordId", recordId)
           .param("step", step))
       .andExpect(status().isNotFound())
       .andExpect(jsonPath("$.message",
           is("record not found")));
  }

  @Test
  void getRecordsTier_expectSuccess() throws Exception {
    RecordTiersInfoDTO recordTiersInfoDTO1 = new RecordTiersInfoDTO.RecordTiersInfoDtoBuilder()
        .setRecordId("recordId")
        .setContentTier(MediaTier.T3)
        .setContentTierBeforeLicenseCorrection(MediaTier.T4)
        .setLicense(LicenseType.OPEN)
        .setMetadataTier(MetadataTier.TA)
        .setMetadataTierLanguage(MetadataTier.TB)
        .setMetadataTierEnablingElements(MetadataTier.TC)
        .setMetadataTierContextualClasses(MetadataTier.T0)
        .build();

    List<RecordTiersInfoDTO> resultMock = List.of(recordTiersInfoDTO1);

//    when(recordService.getRecordsTiers("datasetId")).thenReturn(resultMock);

    mvc.perform(get("/dataset/{id}/records-tiers", "datasetId"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$", hasSize(1)))
       .andExpect(jsonPath("$[0].record-id", is("recordId")))
       .andExpect(jsonPath("$[0].content-tier", is("3")))
       .andExpect(jsonPath("$[0].content-tier-before-license-correction", is("4")))
       .andExpect(jsonPath("$[0].license", is("OPEN")))
       .andExpect(jsonPath("$[0].metadata-tier", is("A")))
       .andExpect(jsonPath("$[0].metadata-tier-language", is("B")))
       .andExpect(jsonPath("$[0].metadata-tier-enabling-elements", is("C")))
       .andExpect(jsonPath("$[0].metadata-tier-contextual-classes", is("0")));

  }

  @Test
  void getRecordsTier_expectInvalidDatasetException() throws Exception {
    InvalidDatasetException invalidDatasetException = new InvalidDatasetException("datasetId");
//    when(recordService.getRecordsTiers("datasetId")).thenThrow(invalidDatasetException);

    mvc.perform(get("/dataset/{id}/records-tiers", "datasetId"))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.message", is("Provided dataset id: [datasetId] is not valid. ")));

  }

  @Test
  void processDatasetFromZipFile_AsyncExecutionException_expectLogging() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    MockMultipartFile mockMultipart = new MockMultipartFile("dataset", "dataset.txt", "application/zip",
        "<test></test>".getBytes());
    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");
    ServiceException exception = new ServiceException("Test error");

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
        .file(mockMultipart)
        .header("Authorization", BEARER + MOCK_VALID_TOKEN)
        .header("User-Agent", "Mozilla")
        .param("country", ITALY.name())
        .param("language", IT.name())
        .param("stepsize", "2"));
  }

  @Test
  void processDatasetFromURL_AsyncExecutionException_expectLogging() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    ServiceException exception = new ServiceException("Test error");
    String url = Paths.get("zip", "dataset-valid.zip").toUri().toString();
    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
        .header("Authorization", BEARER + MOCK_VALID_TOKEN)
        .header("User-Agent", "Mozilla")
        .param("country", ITALY.name())
        .param("language", IT.name())
        .param("url", url)
        .param("stepsize", "2"));
  }

  @Test
  void processDatasetFromOAI_AsyncExecutionException_expectLogging() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
    ServiceException exception = new ServiceException("Test error");
    final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();
    when(datasetService.createDataset(any(), eq("my-data-set"), anyString(), eq(ITALY),
        eq(IT), anyString()))
        .thenReturn("12345");

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
        .header("Authorization", BEARER + MOCK_VALID_TOKEN)
        .header("User-Agent", "Mozilla")
        .param("country", ITALY.xmlValue())
        .param("language", IT.xmlValue())
        .param("url", url)
        .param("setspec", "1073")
        .param("metadataformat", "rdf")
        .param("stepsize", "2"));
  }
}
