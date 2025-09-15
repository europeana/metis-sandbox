package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarning;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningRepository;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressByStepDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionStatus;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.entity.XsltType;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.DatasetRepository.DatasetIdProjection;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.sandbox.service.engine.WorkflowHelper;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DatasetReportServiceTest {

  @Mock
  private ExecutionRecordRepository executionRecordRepository;
  @Mock
  private ExecutionRecordErrorRepository executionRecordErrorRepository;
  @Mock
  private ExecutionRecordWarningRepository executionRecordWarningRepository;
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
  void findDatasetIdsByCreatedBefore_shouldReturnDatasetIds() {
    int days = 10;
    ZonedDateTime expectedDate = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(days);

    List<String> datasetIds = List.of("111", "222");
    List<DatasetIdProjection> datasetIdProjections =
        datasetIds.stream()
                  .map(datasetId -> (DatasetIdProjection) () -> Integer.valueOf(datasetId))
                  .toList();

    when(datasetRepository.findByCreatedDateBefore(any(ZonedDateTime.class))).thenReturn(datasetIdProjections);
    List<String> result = datasetReportService.findDatasetIdsByCreatedBefore(days);
    assertEquals(datasetIds, result);

    ArgumentCaptor<ZonedDateTime> zonedDateTimeArgumentCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
    verify(datasetRepository, times(1)).findByCreatedDateBefore(zonedDateTimeArgumentCaptor.capture());
    ZonedDateTime actualArgument = zonedDateTimeArgumentCaptor.getValue();

    // Allow a small tolerance window because ZonedDateTime.now() is evaluated per line
    long secondsDifference = Math.abs(Duration.between(expectedDate, actualArgument).getSeconds());
    assertTrue(secondsDifference < 10);
  }

  @Test
  void findDatasetIdsByCreatedBefore_shouldThrowServiceExceptionWhenRepositoryThrows() {
    int days = 5;
    when(datasetRepository.findByCreatedDateBefore(any(ZonedDateTime.class))).thenThrow(new RuntimeException());
    assertThrows(ServiceException.class, () -> datasetReportService.findDatasetIdsByCreatedBefore(days));
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

    OaiHarvestParametersEntity oaiHarvestParametersEntity = new OaiHarvestParametersEntity();
    oaiHarvestParametersEntity.setUrl("url");
    oaiHarvestParametersEntity.setSetSpec("setSpec");
    oaiHarvestParametersEntity.setMetadataFormat("metadataFormat");
    oaiHarvestParametersEntity.setStepSize(1);

    when(datasetRepository.findById(datasetEntity.getDatasetId())).thenReturn(Optional.of(datasetEntity));
    when(transformXsltRepository.findByDatasetId(valueOf(datasetEntity.getDatasetId()))).thenReturn(
        Optional.of(new TransformXsltEntity()));
    when(harvestParameterService.getDatasetHarvestingParameters(valueOf(datasetEntity.getDatasetId()))).thenReturn(
        Optional.of(oaiHarvestParametersEntity));

    DatasetInfoDTO datasetInfo = datasetReportService.getDatasetInfo(valueOf(datasetEntity.getDatasetId()));

    assertNotNull(datasetInfo);
    assertEquals(valueOf(datasetEntity.getDatasetId()), datasetInfo.getDatasetId());
    assertEquals(dateNow, datasetInfo.getCreationDate());
    assertEquals(datasetEntity.getDatasetName(), datasetInfo.getDatasetName());

    assertEquals(Language.EL, datasetInfo.getLanguage());
    assertEquals(Country.GREECE, datasetInfo.getCountry());
    assertEquals(datasetEntity.getCreatedById(), datasetInfo.getCreatedById());
    assertTrue(datasetInfo.isTransformedToEdmExternal());

    assertNotNull(datasetInfo.getAbstractHarvestParametersDTO());
    OaiHarvestParametersDTO oaiHarvestParametersDTO = assertInstanceOf(OaiHarvestParametersDTO.class, datasetInfo.getAbstractHarvestParametersDTO());
    assertEquals(oaiHarvestParametersEntity.getUrl(), oaiHarvestParametersDTO.getUrl());
    assertEquals(oaiHarvestParametersEntity.getSetSpec(), oaiHarvestParametersDTO.getSetSpec());
    assertEquals(oaiHarvestParametersEntity.getMetadataFormat(), oaiHarvestParametersDTO.getMetadataFormat());
    assertEquals(oaiHarvestParametersEntity.getStepSize(), oaiHarvestParametersDTO.getStepSize());
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
      when(executionRecordErrorRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId, stepName))
          .thenReturn(totalFailInStep);
      when(executionRecordWarningRepository.countByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(
          datasetId, stepName)).thenReturn(totalWarningInStep);

      ExecutionRecordIdentifierKey identifierKey = new ExecutionRecordIdentifierKey();
      identifierKey.setDatasetId(datasetId);
      identifierKey.setExecutionName(stepName);
      identifierKey.setSourceRecordId("sourceRecordId");
      identifierKey.setRecordId("recordId");

      ExecutionRecord executionRecord = new ExecutionRecord();
      executionRecord.setIdentifier(identifierKey);

      ExecutionRecordWarning executionRecordWarning = new ExecutionRecordWarning();
      executionRecordWarning.setExecutionRecord(executionRecord);
      executionRecordWarning.setException("exception");
      executionRecordWarning.setMessage("warning");

      when(executionRecordErrorRepository.findByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId, stepName))
          .thenReturn(List.of());
      when(executionRecordWarningRepository.findByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(
          datasetId, stepName)).thenReturn(List.of(executionRecordWarning));
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
