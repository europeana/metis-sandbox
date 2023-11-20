package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.RecordTiersInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.TierStatistics;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfo;
import eu.europeana.metis.sandbox.service.dataset.DatasetLogService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.record.RecordService;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService;
import eu.europeana.metis.sandbox.service.workflow.HarvestPublishService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import eu.europeana.metis.utils.CompressedFileExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.multipart.MultipartFile;


@ExtendWith(SpringExtension.class)
@WebMvcTest(DatasetController.class)
class DatasetControllerTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private RateLimitInterceptor rateLimitInterceptor;

  @MockBean
  private DatasetService datasetService;

  @MockBean
  private DatasetLogService datasetLogService;

  @MockBean
  private DatasetReportService datasetReportService;

  @MockBean
  private RecordService recordService;

  @MockBean
  private RecordLogService recordLogService;

  @MockBean
  private RecordTierCalculationService recordTierCalculationService;

  @MockBean
  private HarvestPublishService harvestPublishService;

  @Mock
  private CompletableFuture<Void> asyncResult;

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
        arguments(null, Set.of(Step.HARVEST_FILE, Step.HARVEST_OAI_PMH), status().isOk(),
            content().string("exampleString")),
        arguments("", Set.of(Step.HARVEST_FILE, Step.HARVEST_OAI_PMH), status().isOk(),
            content().string("exampleString")),
        arguments("HARVEST", Set.of(Step.HARVEST_FILE, Step.HARVEST_OAI_PMH), status().isOk(),
            content().string("exampleString")),
        arguments("TRANSFORM_TO_EDM_EXTERNAL", Set.of(Step.TRANSFORM_TO_EDM_EXTERNAL),
            status().isOk(), content().string("exampleString")),
        arguments("VALIDATE_EXTERNAL", Set.of(Step.VALIDATE_EXTERNAL), status().isOk(),
            content().string("exampleString")),
        arguments("TRANSFORM", Set.of(Step.TRANSFORM), status().isOk(),
            content().string("exampleString")),
        arguments("VALIDATE_INTERNAL", Set.of(Step.VALIDATE_INTERNAL), status().isOk(),
            content().string("exampleString")),
        arguments("NORMALIZE", Set.of(Step.NORMALIZE), status().isOk(),
            content().string("exampleString")),
        arguments("ENRICH", Set.of(Step.ENRICH), status().isOk(),
            content().string("exampleString")),
        arguments("MEDIA_PROCESS", Set.of(Step.MEDIA_PROCESS), status().isOk(),
            content().string("exampleString")),
        arguments("PUBLISH", Set.of(Step.PUBLISH), status().isOk(),
            content().string("exampleString")),
        arguments("CLOSE", Set.of(Step.CLOSE), status().isOk(), content().string("exampleString")),
        arguments("NON_SENSE", Set.of(), status().isBadRequest(), content().string("{\"statusCode\":400,\"status\":\"BAD_REQUEST\",\"message\":\"Invalid step name NON_SENSE\"}"))
    );
  }

  @BeforeEach
  public void setup() {
    when(harvestPublishService.runHarvestFileAsync(any(), any(), any())).thenReturn(asyncResult);
    when(harvestPublishService.runHarvestHttpFileAsync(any(), any(), any())).thenReturn(asyncResult);
    when(harvestPublishService.runHarvestOaiPmhAsync(any(), any())).thenReturn(asyncResult);
  }

  @ParameterizedTest
  @MethodSource("provideDifferentCompressedFiles")
  void processDatasetFromZipFile_withoutXsltFile_expectSuccess(MockMultipartFile mockMultipart) throws Exception {

    when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT),
        any(ByteArrayInputStream.class)))
        .thenReturn("12345");

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
            .file(mockMultipart)
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("stepsize", "2"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.dataset-id", is("12345")));

    verify(datasetLogService, never()).logException(any(), any());
  }

  @ParameterizedTest
  @MethodSource("provideDifferentCompressedFiles")
  void processDatasetFromZipFile_withXsltFile_expectSuccess(MockMultipartFile mockMultipart) throws Exception {

    MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());

    when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT),
        any(ByteArrayInputStream.class)))
        .thenReturn("12345");

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
            .file(mockMultipart)
            .file(xsltMock)
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("stepsize", "2"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.dataset-id", is("12345")));

    verify(datasetLogService, never()).logException(any(), any());
  }

  @ParameterizedTest
  @MethodSource("provideDifferentUrlsOfCompressedFiles")
  void processDatasetFromURL_withoutXsltFile_expectSuccess(String url) throws Exception {

    when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT),
        any(ByteArrayInputStream.class)))
        .thenReturn("12345");

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url)
            .param("stepsize", "2"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.dataset-id", is("12345")));

    verify(datasetLogService, never()).logException(any(), any());
  }

  @ParameterizedTest
  @MethodSource("provideDifferentUrlsOfCompressedFiles")
  void processDatasetFromURL_withXsltFile_expectSuccess(String url) throws Exception {

    MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());

    when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT),
        any(ByteArrayInputStream.class)))
        .thenReturn("12345");

    mvc.perform(multipart("/dataset/{name}/harvestByUrl", "my-data-set")
            .file(xsltMock)
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url)
            .param("stepsize", "2"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.dataset-id", is("12345")));

    verify(datasetLogService, never()).logException(any(), any());
  }

  @Test
  void processDatasetFromOAI_expectSuccess() throws Exception {

    final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

    when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT),
        any(ByteArrayInputStream.class)))
        .thenReturn("12345");

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
            .param("country", ITALY.xmlValue())
            .param("language", IT.xmlValue())
            .param("url", url)
            .param("setspec", "1073")
            .param("metadataformat", "rdf")
            .param("stepsize", "2"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.dataset-id", is("12345")));

    verify(datasetLogService, never()).logException(any(), any());
  }

  @Test
  void processDatasetFromOAIWithXsltFile_expectSuccess() throws Exception {

    final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

    MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl",
        "application/xslt+xml",
        "string".getBytes());

    when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT),
        any(InputStream.class)))
        .thenReturn("12345");

    mvc.perform(multipart("/dataset/{name}/harvestOaiPmh", "my-data-set")
            .file(xsltMock)
            .param("country", ITALY.xmlValue())
            .param("language", IT.xmlValue())
            .param("url", url)
            .param("setspec", "1073")
            .param("metadataformat", "rdf")
            .param("stepsize", "2"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.dataset-id", is("12345")));

    verify(datasetLogService, never()).logException(any(), any());
  }

  @Test
  void processDatasetFromFile_invalidName_expectFail() throws Exception {

    var dataset = new MockMultipartFile("dataset", "dataset.txt", "application/zip",
        "<test></test>".getBytes());

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data=set")
            .file(dataset)
            .param("country", ITALY.name())
            .param("language", IT.name()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            is("dataset name can only include letters, numbers, _ or - characters")));
  }

  @Test
  void processDatasetFromFile_invalidStepSize_expectFail() throws Exception {

    var dataset = new MockMultipartFile("dataset", "dataset.txt", "application/zip",
        "<test></test>".getBytes());

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
            .file(dataset)
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("stepsize", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            is("Step size must be a number higher than zero")));
  }

  @Test
  void processDatasetFromFile_invalidFileType_expectFail() throws Exception {

    var dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
            "<test></test>".getBytes());

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
                    .file(dataset)
                    .param("country", ITALY.name())
                    .param("language", IT.name())
                    .param("stepsize", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message",
                    is("File provided is not valid compressed file. ")));
  }

  @Test
  void processDatasetFromURL_invalidName_expectFail() throws Exception {

    final String url = "zip" + File.separator + "dataset-valid.zip";

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data=set")
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

    final String url = Paths.get("zip" + File.separator + "dataset-valid.zip").toUri().toString();

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
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

    final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data=set")
            .param("name", "invalidDatasetName")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url)
            .param("setspec", "1073")
            .param("metadataformat", "rdf"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            is("dataset name can only include letters, numbers, _ or - characters")));
  }

  @Test
  void processDatasetFromOAI_invalidStepSize_expectFail() throws Exception {

    final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
            .param("name", "invalidDatasetName")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url)
            .param("setspec", "1073")
            .param("metadataformat", "rdf")
            .param("stepsize", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            is("Step size must be a number higher than zero")));
  }

  @Test
  void processDatasetFromOAI_harvestServiceFails_expectFail() throws Exception {

    final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

    when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT),
        any(InputStream.class)))
        .thenReturn("12345");
    doThrow(new IllegalArgumentException(new Exception())).when(harvestPublishService)
        .runHarvestOaiPmhAsync(any(DatasetMetadata.class),
            any(OaiHarvestData.class));

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url)
            .param("setspec", "1073")
            .param("metadataformat", "rdf"))
        .andExpect(status().isBadRequest())
        .andExpect(result -> assertTrue(
            result.getResolvedException() instanceof IllegalArgumentException));
  }

  @Test
  void processDatasetFromOAI_datasetServiceFails_expectFail() throws Exception {

    final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

    when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT),
        any(InputStream.class)))
        .thenThrow(new ServiceException("Failed", new Exception()));

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url)
            .param("setspec", "1073")
            .param("metadataformat", "rdf"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message",
            is("Failed Please retry, if problem persists contact provider.")));
  }


  @Test
  void processDatasetFromOAI_differentXsltFileType_expectFail() throws Exception {

    final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();

    MockMultipartFile xsltMock = new MockMultipartFile("xsltFile", "xslt.xsl", "application/zip",
        "string".getBytes());

    mvc.perform(multipart("/dataset/{name}/harvestOaiPmh", "my-data-set")
            .file(xsltMock)
            .param("country", ITALY.name())
            .param("language", IT.name())
            .param("url", url)
            .param("setspec", "1073")
            .param("metadataformat", "rdf"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            is("The given xslt file should be a single xml file.")));
  }

  @Test
  void retrieveDataset_expectSuccess() throws Exception {
    var message1 = "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.";
    var message2 = "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.";
    var error1 = new ErrorInfoDto(message1, Status.FAIL, List.of("1", "2"));
    var error2 = new ErrorInfoDto(message2, Status.FAIL, List.of("3", "4"));
    var errors = List.of(error1, error2);
    var createProgress = new ProgressByStepDto(Step.HARVEST_FILE, 10, 0, 0, List.of());
    var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 7, 3, 0, errors);
    var tiersZeroInfo = new TiersZeroInfo(new TierStatistics(0, Collections.emptyList()),
        new TierStatistics(0, Collections.emptyList()));
    var report = new ProgressInfoDto("https://metis-sandbox",
        10L, 10L, List.of(createProgress, externalProgress), false, "", emptyList(),
        tiersZeroInfo);
    when(datasetReportService.getReport("1")).thenReturn(report);

    mvc.perform(get("/dataset/{id}/progress", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status",
            is("COMPLETED")))
        .andExpect(jsonPath("$.progress-by-step[1].errors[0].message",
            is(message1)));

    verify(datasetLogService, never()).logException(any(), any());
  }

  @Test
  void retrieveDataset_datasetInvalidDatasetId_expectFail() throws Exception {

    when(datasetReportService.getReport("1"))
        .thenThrow(new InvalidDatasetException("1"));

    mvc.perform(get("/dataset/{id}/progress", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message",
            is("Provided dataset id: [1] is not valid. ")));
  }

  @Test
  void retrieveDataset_datasetReportServiceFails_expectFail() throws Exception {

    when(datasetReportService.getReport("1"))
        .thenThrow(new ServiceException("Failed", new Exception()));

    mvc.perform(get("/dataset/{id}/progress", "1"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message",
            is("Failed Please retry, if problem persists contact provider.")));
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

    verify(datasetLogService, never()).logException(any(), any());
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
  void getRecord_expectSuccess(String step, Set<Step> steps, ResultMatcher expectedStatus,
      ResultMatcher expectedContent) throws Exception {
    final String datasetId = "1";
    final String recordId = "europeanaId";
    final String returnString = "exampleString";
    when(recordLogService.getProviderRecordString(recordId, datasetId, steps))
        .thenReturn(returnString);

    mvc.perform(get("/dataset/{id}/record", datasetId)
            .param("recordId", recordId)
            .param("step", step))
        .andExpect(expectedStatus)
        .andExpect(expectedContent);

    verify(datasetLogService, never()).logException(any(), any());
  }

  @Test
  void getRecord_stepOptional_expectSuccess() throws Exception {
    final String datasetId = "1";
    final String recordId = "europeanaId";
    final String returnString = "exampleString";
    when(recordLogService.getProviderRecordString(recordId, datasetId,
        Set.of(Step.HARVEST_FILE, Step.HARVEST_OAI_PMH)))
        .thenReturn(returnString);

    mvc.perform(get("/dataset/{id}/record", datasetId)
            .param("recordId", recordId))
        .andExpect(content().string(returnString));

    verify(datasetLogService, never()).logException(any(), any());
  }

  @Test
  void getRecord_NoRecordFoundException() throws Exception {
    final String datasetId = "1";
    final String recordId = "europeanaId";
    final String step = "HARVEST";
    when(recordLogService.getProviderRecordString(anyString(), anyString(),
        any(Set.class))).thenThrow(
        new NoRecordFoundException("record not found"));

    mvc.perform(get("/dataset/{id}/record", datasetId)
            .param("recordId", recordId)
            .param("step", step))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message",
            is("record not found")));
  }

  @Test
  void getRecordsTier_expectSuccess() throws Exception {
    RecordTiersInfoDto recordTiersInfoDto1 = new RecordTiersInfoDto.RecordTiersInfoDtoBuilder()
            .setRecordId("recordId")
            .setContentTier(MediaTier.T3)
            .setContentTierBeforeLicenseCorrection(MediaTier.T4)
            .setLicense(LicenseType.OPEN)
            .setMetadataTier(MetadataTier.TA)
            .setMetadataTierLanguage(MetadataTier.TB)
            .setMetadataTierEnablingElements(MetadataTier.TC)
            .setMetadataTierContextualClasses(MetadataTier.T0)
            .build();

    List<RecordTiersInfoDto> resultMock = List.of(recordTiersInfoDto1);

    when(recordService.getRecordsTiers("datasetId")).thenReturn(resultMock);

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
    when(recordService.getRecordsTiers("datasetId")).thenThrow(invalidDatasetException);

    mvc.perform(get("/dataset/{id}/records-tiers", "datasetId"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("Provided dataset id: [datasetId] is not valid. ")));

  }

  @Test
  void processDatasetFromZipFile_AsyncExecutionException_expectLogging() throws Exception {
    MockMultipartFile mockMultipart = new MockMultipartFile("dataset", "dataset.txt", "application/zip",
        "<test></test>".getBytes());
    when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT),
        any(ByteArrayInputStream.class)))
        .thenReturn("12345");
    ServiceException exception = new ServiceException("Test error");
    when(harvestPublishService.runHarvestFileAsync(any(), any(), eq(CompressedFileExtension.ZIP))).thenReturn(
        CompletableFuture.failedFuture(exception));

    mvc.perform(multipart("/dataset/{name}/harvestByFile", "my-data-set")
        .file(mockMultipart)
        .param("country", ITALY.name())
        .param("language", IT.name())
        .param("stepsize", "2"));

    verify(datasetLogService).logException("12345", exception);
  }

  @Test
  void processDatasetFromURL_AsyncExecutionException_expectLogging() throws Exception {
    ServiceException exception = new ServiceException("Test error");
    when(harvestPublishService.runHarvestHttpFileAsync(any(), any(), any())).thenReturn(
        CompletableFuture.failedFuture(exception));
    String url = Paths.get("zip", "dataset-valid.zip").toUri().toString();
    when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT),
        any(ByteArrayInputStream.class)))
        .thenReturn("12345");

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
        .param("country", ITALY.name())
        .param("language", IT.name())
        .param("url", url)
        .param("stepsize", "2"));

    verify(datasetLogService).logException("12345", exception);
  }

  @Test
  void processDatasetFromOAI_AsyncExecutionException_expectLogging() throws Exception {
    ServiceException exception = new ServiceException("Test error");
    when(harvestPublishService.runHarvestOaiPmhAsync(any(), any())).thenReturn(
        CompletableFuture.failedFuture(exception));
    final String url = new URI("http://panic.image.ntua.gr:9000/efg/oai").toString();
    when(datasetService.createEmptyDataset(eq("my-data-set"), eq(ITALY), eq(IT),
        any(ByteArrayInputStream.class)))
        .thenReturn("12345");

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
        .param("country", ITALY.xmlValue())
        .param("language", IT.xmlValue())
        .param("url", url)
        .param("setspec", "1073")
        .param("metadataformat", "rdf")
        .param("stepsize", "2"));

    verify(datasetLogService).logException("12345", exception);
  }
}
