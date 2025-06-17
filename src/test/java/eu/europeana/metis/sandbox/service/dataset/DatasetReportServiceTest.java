package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarningException;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningExceptionRepository;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestDTO;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressByStepDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionStatus;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.entity.XsltType;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParameters;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.sandbox.service.engine.WorkflowHelper;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DatasetReportServiceTest {

  @Mock
  private ExecutionRecordRepository executionRecordRepository;
  @Mock
  private ExecutionRecordExceptionRepository executionRecordExceptionRepository;
  @Mock
  private ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository;
  @Mock
  private ExecutionRecordTierContextRepository executionRecordTierContextRepository;
  @Mock
  private DatasetRepository datasetRepository;
  @Mock
  private TransformXsltRepository transformXsltRepository;
  @Mock
  private HarvestParameterService harvestParameterService;

  @InjectMocks
  private DatasetReportService datasetReportService;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(datasetReportService, "maxRecords", 1000);
    ReflectionTestUtils.setField(datasetReportService, "portalPublishDatasetUrl", "http://test/");
  }

  @Test
  void getDatasetInfo() {
    ZonedDateTime dateNow = ZonedDateTime.now();
    DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(123);
    datasetEntity.setCreatedDate(dateNow);
    datasetEntity.setDatasetName("datasetName");
    datasetEntity.setWorkflowType(WorkflowType.OAI_HARVEST);
    datasetEntity.setLanguage(Language.EL);
    datasetEntity.setCountry(Country.GREECE);
    datasetEntity.setCreatedById("userId");

    OaiHarvestParameters oaiHarvestParameters = new OaiHarvestParameters();
    oaiHarvestParameters.setUrl("url");
    oaiHarvestParameters.setSetSpec("setSpec");
    oaiHarvestParameters.setMetadataFormat("metadataFormat");
    oaiHarvestParameters.setStepSize(1);

    when(datasetRepository.findById(datasetEntity.getDatasetId())).thenReturn(Optional.of(datasetEntity));
    when(transformXsltRepository.findByDatasetId(valueOf(datasetEntity.getDatasetId()))).thenReturn(
        Optional.of(new TransformXsltEntity()));
    when(harvestParameterService.getDatasetHarvestingParameters(valueOf(datasetEntity.getDatasetId()))).thenReturn(
        Optional.of(oaiHarvestParameters));

    DatasetInfoDTO datasetInfo = datasetReportService.getDatasetInfo(valueOf(datasetEntity.getDatasetId()));

    assertNotNull(datasetInfo);
    assertEquals(valueOf(datasetEntity.getDatasetId()), datasetInfo.getDatasetId());
    assertEquals(dateNow, datasetInfo.getCreationDate());
    assertEquals(datasetEntity.getDatasetName(), datasetInfo.getDatasetName());

    assertEquals(Language.EL, datasetInfo.getLanguage());
    assertEquals(Country.GREECE, datasetInfo.getCountry());
    assertEquals(datasetEntity.getCreatedById(), datasetInfo.getCreatedById());
    assertTrue(datasetInfo.isTransformedToEdmExternal());

    assertNotNull(datasetInfo.getHarvestParametersDto());
    OaiHarvestDTO oaiHarvestDTO = assertInstanceOf(OaiHarvestDTO.class, datasetInfo.getHarvestParametersDto());
    assertEquals(oaiHarvestParameters.getUrl(), oaiHarvestDTO.getUrl());
    assertEquals(oaiHarvestParameters.getSetSpec(), oaiHarvestDTO.getSetSpec());
    assertEquals(oaiHarvestParameters.getMetadataFormat(), oaiHarvestDTO.getMetadataFormat());
    assertEquals(oaiHarvestParameters.getStepSize(), oaiHarvestDTO.getStepSize());
  }

  @Test
  void getDatasetInfo_InvalidDataset() {
    String datasetId = "1";
    when(datasetRepository.findById(Integer.valueOf(datasetId))).thenReturn(Optional.empty());
    assertThrows(InvalidDatasetException.class, () -> datasetReportService.getDatasetInfo(datasetId));
  }

  @Test
  void getProgress() {
    String datasetId = "1";
    ZonedDateTime dateNow = ZonedDateTime.now();
    DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(Integer.valueOf(datasetId));
    datasetEntity.setCreatedDate(dateNow);
    datasetEntity.setDatasetName("datasetName");
    datasetEntity.setWorkflowType(WorkflowType.OAI_HARVEST);
    datasetEntity.setLanguage(Language.EL);
    datasetEntity.setCountry(Country.GREECE);
    datasetEntity.setCreatedById("userId");

    when(datasetRepository.findByDatasetId(Integer.parseInt(datasetId))).thenReturn(Optional.of(datasetEntity));
    TransformXsltEntity transformXsltEntity = new TransformXsltEntity();
    transformXsltEntity.setDatasetId(String.valueOf(datasetId));
    transformXsltEntity.setType(XsltType.EXTERNAL);
    transformXsltEntity.setTransformXslt("transformXslt");
    when(transformXsltRepository.findByDatasetId(valueOf(datasetId))).thenReturn(
        Optional.of(transformXsltEntity));

    long totalSuccessInStep = 10L;
    long totalFailInStep = 0L;
    long totalWarningInStep = 1L;
    List<FullBatchJobType> workflowSteps = WorkflowHelper.getWorkflow(datasetEntity, transformXsltEntity);
    for (FullBatchJobType step : workflowSteps) {
      String stepName = step.name();

      when(executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId, stepName))
          .thenReturn(totalSuccessInStep);
      when(executionRecordExceptionRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId, stepName))
          .thenReturn(totalFailInStep);
      when(executionRecordWarningExceptionRepository.countByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(
          datasetId, stepName)).thenReturn(totalWarningInStep);

      ExecutionRecordIdentifierKey identifierKey = new ExecutionRecordIdentifierKey();
      identifierKey.setDatasetId(datasetId);
      identifierKey.setExecutionName(stepName);
      identifierKey.setSourceRecordId("sourceRecordId");
      identifierKey.setRecordId("recordId");

      ExecutionRecord executionRecord = new ExecutionRecord();
      executionRecord.setIdentifier(identifierKey);

      ExecutionRecordWarningException warning = new ExecutionRecordWarningException();
      warning.setExecutionRecord(executionRecord);
      warning.setException("exception");
      warning.setMessage("warning");

      when(executionRecordExceptionRepository.findByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId, stepName))
          .thenReturn(List.of());
      when(executionRecordWarningExceptionRepository.findByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(
          datasetId, stepName)).thenReturn(List.of(warning));
    }
    ExecutionProgressInfoDTO executionProgressInfoDTO = datasetReportService.getProgress(valueOf(datasetEntity.getDatasetId()));

    assertNotNull(executionProgressInfoDTO);
    assertTrue(StringUtils.isNotBlank(executionProgressInfoDTO.portalPublishUrl()));
    assertEquals(ExecutionStatus.COMPLETED, executionProgressInfoDTO.executionStatus());
    assertEquals(totalSuccessInStep, executionProgressInfoDTO.totalRecords());
    assertEquals(totalSuccessInStep - totalFailInStep, executionProgressInfoDTO.processedRecords());

    for (int i = 0; i < workflowSteps.size(); i++) {
      FullBatchJobType jobType = workflowSteps.get(i);
      ExecutionProgressByStepDTO executionProgressByStepDTO = executionProgressInfoDTO.executionProgressByStepDTOS().get(i);
      assertEquals(jobType, executionProgressByStepDTO.step());
      assertEquals(totalSuccessInStep, executionProgressByStepDTO.total());
      assertEquals(totalSuccessInStep, executionProgressByStepDTO.success());
      assertEquals(totalFailInStep, executionProgressByStepDTO.fail());
      assertEquals(totalWarningInStep, executionProgressByStepDTO.warn());
      List<ErrorInfoDTO> errors = executionProgressByStepDTO.errors();
      assertEquals(totalWarningInStep + totalFailInStep, errors.size());
      assertEquals(Status.WARN, errors.getFirst().type());
      assertEquals("warning", errors.getFirst().errorMessage());
      assertEquals(List.of("recordId | sourceRecordId"), errors.getFirst().recordIds());
    }
    assertFalse(executionProgressInfoDTO.recordLimitReached());
    assertNull(executionProgressInfoDTO.tiersZeroInfoDTO());
  }

}
