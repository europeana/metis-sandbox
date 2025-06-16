//package eu.europeana.metis.sandbox.controller;
//
//import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
//import static eu.europeana.metis.sandbox.common.locale.Language.IT;
//import static eu.europeana.metis.security.test.JwtUtils.BEARER;
//import static eu.europeana.metis.security.test.JwtUtils.MOCK_VALID_TOKEN;
//import static java.util.Collections.emptyList;
//import static org.hamcrest.Matchers.hasSize;
//import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.isEmptyOrNullString;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.junit.jupiter.params.provider.Arguments.arguments;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.ArgumentMatchers.isNull;
//import static org.mockito.Mockito.reset;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//import eu.europeana.indexing.tiers.model.MediaTier;
//import eu.europeana.indexing.tiers.model.MetadataTier;
//import eu.europeana.indexing.tiers.view.ContentTierBreakdown;
//import eu.europeana.indexing.tiers.view.RecordTierCalculationSummary;
//import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
//import eu.europeana.indexing.utils.LicenseType;
//import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
//import eu.europeana.metis.sandbox.common.Status;
//import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
//import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
//import eu.europeana.metis.sandbox.common.exception.ServiceException;
//import eu.europeana.metis.sandbox.config.SecurityConfig;
//import eu.europeana.metis.sandbox.config.webmvc.WebMvcConfig;
//import eu.europeana.metis.sandbox.controller.advice.RestResponseExceptionHandler;
//import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
//import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
//import eu.europeana.metis.sandbox.dto.harvest.FileHarvestDTO;
//import eu.europeana.metis.sandbox.dto.harvest.HttpHarvestDTO;
//import eu.europeana.metis.sandbox.dto.OAIPmhHarvestDTO;
//import eu.europeana.metis.sandbox.dto.RecordTiersInfoDTO;
//import eu.europeana.metis.sandbox.dto.report.ErrorInfoDTO;
//import eu.europeana.metis.sandbox.dto.report.ProgressByStepDTO;
//import eu.europeana.metis.sandbox.dto.report.ProgressInfoDTO;
//import eu.europeana.metis.sandbox.dto.report.TierStatisticsDTO;
//import eu.europeana.metis.sandbox.dto.report.TiersZeroInfoDTO;
//import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
//import eu.europeana.metis.sandbox.service.dataset.DatasetService;
//import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService;
//import eu.europeana.metis.security.test.JwtUtils;
//import java.io.File;
//import java.net.URI;
//import java.nio.file.Paths;
//import java.time.Instant;
//import java.time.ZoneOffset;
//import java.time.ZonedDateTime;
//import java.util.Collections;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.CompletableFuture;
//import java.util.stream.Stream;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.mockito.Mock;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.security.oauth2.jwt.JwtDecoder;
//import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultMatcher;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//import org.springframework.web.multipart.MultipartFile;
//
//@WebMvcTest(DatasetController.class)
//@ContextConfiguration(classes = {WebMvcConfig.class, DatasetController.class, SecurityConfig.class, RestResponseExceptionHandler.class})
//class DatasetControllerTest {
//
//  @MockBean
//  private RateLimitInterceptor rateLimitInterceptor;
//
//  @MockBean
//  private DatasetService datasetService;
//
//  @MockBean
//  private DatasetReportService datasetReportService;
//
//  @MockBean
//  private RecordTierCalculationService recordTierCalculationService;
//
//  @MockBean
//  JwtDecoder jwtDecoder;
//
//  @Mock
//  private CompletableFuture<Void> asyncResult;
//
//  private static MockMvc mvc;
//  private final JwtUtils jwtUtils;
//
//  public DatasetControllerTest() {
//    jwtUtils = new JwtUtils(List.of());
//  }
//
//  @BeforeAll
//  static void setup(WebApplicationContext context) {
//    mvc = MockMvcBuilders.webAppContextSetup(context)
//                         .apply(SecurityMockMvcConfigurers.springSecurity())
//                         .defaultRequest(get("/"))
//                         .build();
//  }
//
//  @BeforeEach
//  public void setup() {
//    reset(jwtDecoder);
//  }
//
//  private static Stream<Arguments> steps() {
//    return Stream.of(
//        arguments(null, Set.of(FullBatchJobType.HARVEST_FILE, FullBatchJobType.HARVEST_OAI), status().isOk(),
//            content().string("exampleString")),
//        arguments("", Set.of(FullBatchJobType.HARVEST_FILE, FullBatchJobType.HARVEST_OAI), status().isOk(),
//            content().string("exampleString")),
//        arguments("HARVEST", Set.of(FullBatchJobType.HARVEST_FILE, FullBatchJobType.HARVEST_OAI), status().isOk(),
//            content().string("exampleString")),
//        arguments("TRANSFORM_TO_EDM_EXTERNAL", Set.of(FullBatchJobType.TRANSFORM_EXTERNAL),
//            status().isOk(), content().string("exampleString")),
//        arguments("VALIDATE_EXTERNAL", Set.of(FullBatchJobType.VALIDATE_EXTERNAL), status().isOk(),
//            content().string("exampleString")),
//        arguments("TRANSFORM", Set.of(FullBatchJobType.TRANSFORM_INTERNAL), status().isOk(),
//            content().string("exampleString")),
//        arguments("VALIDATE_INTERNAL", Set.of(FullBatchJobType.VALIDATE_INTERNAL), status().isOk(),
//            content().string("exampleString")),
//        arguments("NORMALIZE", Set.of(FullBatchJobType.NORMALIZE), status().isOk(),
//            content().string("exampleString")),
//        arguments("ENRICH", Set.of(FullBatchJobType.ENRICH), status().isOk(),
//            content().string("exampleString")),
//        arguments("MEDIA_PROCESS", Set.of(FullBatchJobType.MEDIA), status().isOk(),
//            content().string("exampleString")),
//        arguments("PUBLISH", Set.of(FullBatchJobType.INDEX), status().isOk(),
//            content().string("exampleString")),
//        arguments("NON_SENSE", Set.of(), status().isBadRequest(),
//            content().string("{\"statusCode\":400,\"status\":\"BAD_REQUEST\",\"message\":\"Invalid step name NON_SENSE\"}"))
//    );
//  }
//
//  @Test
//  void computeRecordTierCalculation_expectSuccess() throws Exception {
//    final String datasetId = "1";
//    final String recordId = "recordId";
//    final String europeanaId = "europeanaId";
//
//    final RecordTierCalculationSummary recordTierCalculationSummary = new RecordTierCalculationSummary();
//    recordTierCalculationSummary.setEuropeanaRecordId(europeanaId);
//    final RecordTierCalculationView recordTierCalculationView = new RecordTierCalculationView(
//        recordTierCalculationSummary,
//        new ContentTierBreakdown.Builder().build(), null);
//    when(recordTierCalculationService.calculateTiers(recordId, datasetId)).thenReturn(
//        recordTierCalculationView);
//
//    mvc.perform(get("/dataset/{id}/record/compute-tier-calculation", datasetId)
//           .param("recordId", recordId))
//       .andExpect(jsonPath("$.recordTierCalculationSummary.europeanaRecordId", is("europeanaId")))
//       .andExpect(jsonPath("$.recordTierCalculationSummary.contentTier", isEmptyOrNullString()));
//  }
//
//  @Test
//  void computeRecordTierCalculation_NoRecordFoundException() throws Exception {
//    final String datasetId = "1";
//    final String recordId = "recordId";
//    when(recordTierCalculationService.calculateTiers(anyString(), anyString())).thenThrow(
//        new NoRecordFoundException("record not found"));
//    mvc.perform(get("/dataset/{id}/record/compute-tier-calculation", datasetId)
//           .param("recordId", recordId))
//       .andExpect(status().isNotFound())
//       .andExpect(jsonPath("$.message",
//           is("record not found")));
//  }
//
//  @ParameterizedTest
//  @MethodSource("steps")
//  void getRecord_expectSuccess(String step, Set<FullBatchJobType> steps, ResultMatcher expectedStatus,
//      ResultMatcher expectedContent) throws Exception {
//    final String datasetId = "1";
//    final String recordId = "europeanaId";
//    final String returnString = "exampleString";
////    when(recordLogService.getProviderRecordString(recordId, datasetId, steps))
////        .thenReturn(returnString);
//
//    mvc.perform(get("/dataset/{id}/record", datasetId)
//           .param("recordId", recordId)
//           .param("step", step))
//       .andExpect(expectedStatus)
//       .andExpect(expectedContent);
//  }
//
//  @Test
//  void getRecord_stepOptional_expectSuccess() throws Exception {
//    final String datasetId = "1";
//    final String recordId = "europeanaId";
//    final String returnString = "exampleString";
////    when(recordLogService.getProviderRecordString(recordId, datasetId,
////        Set.of(Step.HARVEST_FILE, Step.HARVEST_OAI_PMH)))
////        .thenReturn(returnString);
//
//    mvc.perform(get("/dataset/{id}/record", datasetId)
//           .param("recordId", recordId))
//       .andExpect(content().string(returnString));
//  }
//
//  @Test
//  void getRecord_NoRecordFoundException() throws Exception {
//    final String datasetId = "1";
//    final String recordId = "europeanaId";
//    final String step = "HARVEST";
////    when(recordLogService.getProviderRecordString(anyString(), anyString(),
////        any(Set.class))).thenThrow(
////        new NoRecordFoundException("record not found"));
//
//    mvc.perform(get("/dataset/{id}/record", datasetId)
//           .param("recordId", recordId)
//           .param("step", step))
//       .andExpect(status().isNotFound())
//       .andExpect(jsonPath("$.message",
//           is("record not found")));
//  }
//
//  @Test
//  void getRecordsTier_expectSuccess() throws Exception {
//    RecordTiersInfoDTO recordTiersInfoDTO1 = new RecordTiersInfoDTO.RecordTiersInfoDtoBuilder()
//        .setRecordId("recordId")
//        .setContentTier(MediaTier.T3)
//        .setContentTierBeforeLicenseCorrection(MediaTier.T4)
//        .setLicense(LicenseType.OPEN)
//        .setMetadataTier(MetadataTier.TA)
//        .setMetadataTierLanguage(MetadataTier.TB)
//        .setMetadataTierEnablingElements(MetadataTier.TC)
//        .setMetadataTierContextualClasses(MetadataTier.T0)
//        .build();
//
//    List<RecordTiersInfoDTO> resultMock = List.of(recordTiersInfoDTO1);
//
////    when(recordService.getRecordsTiers("datasetId")).thenReturn(resultMock);
//
//    mvc.perform(get("/dataset/{id}/records-tiers", "datasetId"))
//       .andExpect(status().isOk())
//       .andExpect(jsonPath("$", hasSize(1)))
//       .andExpect(jsonPath("$[0].record-id", is("recordId")))
//       .andExpect(jsonPath("$[0].content-tier", is("3")))
//       .andExpect(jsonPath("$[0].content-tier-before-license-correction", is("4")))
//       .andExpect(jsonPath("$[0].license", is("OPEN")))
//       .andExpect(jsonPath("$[0].metadata-tier", is("A")))
//       .andExpect(jsonPath("$[0].metadata-tier-language", is("B")))
//       .andExpect(jsonPath("$[0].metadata-tier-enabling-elements", is("C")))
//       .andExpect(jsonPath("$[0].metadata-tier-contextual-classes", is("0")));
//
//  }
//
//  @Test
//  void getRecordsTier_expectInvalidDatasetException() throws Exception {
//    InvalidDatasetException invalidDatasetException = new InvalidDatasetException("datasetId");
////    when(recordService.getRecordsTiers("datasetId")).thenThrow(invalidDatasetException);
//
//    mvc.perform(get("/dataset/{id}/records-tiers", "datasetId"))
//       .andExpect(status().isBadRequest())
//       .andExpect(jsonPath("$.message", is("Provided dataset id: [datasetId] is not valid. ")));
//
//  }
//}
