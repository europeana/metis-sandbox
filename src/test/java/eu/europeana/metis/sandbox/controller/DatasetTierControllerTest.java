package eu.europeana.metis.sandbox.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.config.SecurityConfig;
import eu.europeana.metis.sandbox.config.webmvc.WebMvcConfig;
import eu.europeana.metis.sandbox.controller.advice.RestResponseExceptionHandler;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DatasetTierController.class)
@ContextConfiguration(classes = {WebMvcConfig.class, DatasetTierController.class, SecurityConfig.class,
    RestResponseExceptionHandler.class})
class DatasetTierControllerTest {

  private static final String DATASET_ID = "datasetId";
  private static final String RECORD_ID = "recordId";
  private static final String EUROPEANA_ID = "europeanaId";

  @MockitoBean
  private RateLimitInterceptor rateLimitInterceptor;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private ExecutionRecordRepository executionRecordRepository;
  @MockitoBean
  private ExecutionRecordTierContextRepository executionRecordTierContextRepository;
  @MockitoBean
  private RecordTierCalculationService recordTierCalculationService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void computeRecordTierCalculation_expectSuccess() throws Exception {
    final RecordTierCalculationSummary recordTierCalculationSummary = new RecordTierCalculationSummary();
    recordTierCalculationSummary.setEuropeanaRecordId(EUROPEANA_ID);
    final RecordTierCalculationView recordTierCalculationView = new RecordTierCalculationView(
        recordTierCalculationSummary,
        new ContentTierBreakdown.Builder().build(), null);
    when(recordTierCalculationService.calculateTiers(RECORD_ID, DATASET_ID)).thenReturn(
        recordTierCalculationView);

    mockMvc.perform(get("/dataset/{id}/record/compute-tier-calculation", DATASET_ID)
               .param("recordId", RECORD_ID))
           .andExpect(jsonPath("$.recordTierCalculationSummary.europeanaRecordId", is(EUROPEANA_ID)))
           .andExpect(jsonPath("$.recordTierCalculationSummary.contentTier", isEmptyOrNullString()));
  }

  @Test
  void computeRecordTierCalculation_NoRecordFoundException() throws Exception {
    when(recordTierCalculationService.calculateTiers(RECORD_ID, DATASET_ID)).thenThrow(
        new NoRecordFoundException("record not found"));
    mockMvc.perform(get("/dataset/{id}/record/compute-tier-calculation", DATASET_ID)
               .param("recordId", RECORD_ID))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.message",
               is("record not found")));
  }

  @Test
  void getRecordsTier_expectSuccess() throws Exception {
    ExecutionRecordIdentifierKey executionRecordIdentifierKey = new ExecutionRecordIdentifierKey();
    executionRecordIdentifierKey.setDatasetId(DATASET_ID);
    executionRecordIdentifierKey.setRecordId(RECORD_ID);
    executionRecordIdentifierKey.setExecutionId("executionId");
    executionRecordIdentifierKey.setExecutionName("executionName");
    ExecutionRecordTierContext executionRecordTierContext = new ExecutionRecordTierContext();
    executionRecordTierContext.setIdentifier(executionRecordIdentifierKey);

    executionRecordTierContext.setContentTier(MediaTier.T3.toString());
    executionRecordTierContext.setContentTierBeforeLicenseCorrection(MediaTier.T4.toString());
    executionRecordTierContext.setMetadataTier(MetadataTier.TA.toString());
    executionRecordTierContext.setMetadataTierLanguage(MetadataTier.TB.toString());
    executionRecordTierContext.setMetadataTierEnablingElements(MetadataTier.TC.toString());
    executionRecordTierContext.setMetadataTierContextualClasses(MetadataTier.T0.toString());
    executionRecordTierContext.setLicense(LicenseType.OPEN.toString());

    when(executionRecordTierContextRepository.findByIdentifier_DatasetId(DATASET_ID)).thenReturn(
        List.of(executionRecordTierContext));

    mockMvc.perform(get("/dataset/{id}/records-tiers", DATASET_ID))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(1)))
           .andExpect(jsonPath("$[0].record-id", is(RECORD_ID)))
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
    InvalidDatasetException invalidDatasetException = new InvalidDatasetException(DATASET_ID);
    when(executionRecordTierContextRepository.findByIdentifier_DatasetId(DATASET_ID)).thenThrow(invalidDatasetException);

    mockMvc.perform(get("/dataset/{id}/records-tiers", DATASET_ID))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message", is("Provided dataset id: [datasetId] is not valid. ")));
  }

  @ParameterizedTest
  @EnumSource(FullBatchJobType.class)
  void getRecord_expectSuccess(FullBatchJobType step) throws Exception {
    final String returnString = "exampleString";

    ExecutionRecordIdentifierKey executionRecordIdentifierKey = new ExecutionRecordIdentifierKey();
    executionRecordIdentifierKey.setDatasetId(DATASET_ID);
    executionRecordIdentifierKey.setRecordId(RECORD_ID);
    executionRecordIdentifierKey.setExecutionId("executionId");
    executionRecordIdentifierKey.setExecutionName("executionName");
    ExecutionRecord executionRecord = new ExecutionRecord();
    executionRecord.setIdentifier(executionRecordIdentifierKey);
    executionRecord.setRecordData(returnString);
    when(executionRecordRepository.findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionNameIn(
        DATASET_ID, RECORD_ID, Set.of(step.name()))).thenReturn(Set.of(executionRecord));

    mockMvc.perform(get("/dataset/{id}/record", DATASET_ID)
               .param("recordId", RECORD_ID)
               .param("step", step.name()))
           .andExpect(status().isOk())
           .andExpect(content().string(returnString));
  }

  @Test
  void getRecord_NoRecordFoundException() throws Exception {
    final FullBatchJobType step = FullBatchJobType.HARVEST_FILE;

    mockMvc.perform(get("/dataset/{id}/record", DATASET_ID)
               .param("recordId", RECORD_ID)
               .param("step", step.name()))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.message", is(RECORD_ID)));
  }
}