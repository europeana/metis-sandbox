package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.common.WorkflowType;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.ExecutionMetadata;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestParametersDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetExecutionSetupServiceTest {

  @Mock
  private DatasetRepository datasetRepository;
  @Mock
  private HarvestParameterService harvestParameterService;
  @Mock
  private TransformXsltRepository transformXsltRepository;

  @InjectMocks
  private DatasetExecutionSetupService datasetExecutionSetupService;

  private static final DatasetMetadataRequest datasetMetadataRequest =
      DatasetMetadataRequest.builder().datasetName("datasetName").country(Country.GREECE).language(Language.EL).build();

  @Test
  void prepareDatasetAndExecution() {
    String datasetId = "1";
    WorkflowType workflowType = WorkflowType.OAI_HARVEST;
    String userId = "userId";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "setStep", "metadataFormat", 1);
    HarvestParametersEntity harvestParameters = mock(HarvestParametersEntity.class);
    AtomicReference<DatasetEntity> datasetEntityReference = new AtomicReference<>();
    when(datasetRepository.save(any())).thenAnswer(invocation -> {
      DatasetEntity datasetEntity = invocation.getArgument(0);
      datasetEntity.setDatasetId(Integer.valueOf(datasetId));
      datasetEntityReference.set(datasetEntity);
      return datasetEntity;
    });
    when(datasetRepository.findById(Integer.valueOf(datasetId))).thenAnswer(
        invocation -> Optional.ofNullable(datasetEntityReference.get()));
    String xsltFile = "string";

    when(harvestParameterService.createDatasetHarvestParameters(datasetId, oaiHarvestParametersDTO)).thenReturn(
        harvestParameters);
    when(transformXsltRepository.save(any(TransformXsltEntity.class))).thenReturn(new TransformXsltEntity());

    ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetAndExecution(
        workflowType, datasetMetadataRequest, userId, xsltFile, oaiHarvestParametersDTO);

    assertNotNull(executionMetadata);
    assertEquals(datasetId, executionMetadata.getDatasetMetadata().getDatasetId());
    assertNotNull(executionMetadata.getInputMetadata().getTransformXsltEntity());
    assertEquals(harvestParameters, executionMetadata.getInputMetadata().getHarvestParametersEntity());
    verify(transformXsltRepository).save(any(TransformXsltEntity.class));
  }

  @Test
  void prepareDatasetAndExecution_withoutXslt() {
    String datasetId = "1";
    WorkflowType workflowType = WorkflowType.OAI_HARVEST;
    String userId = "userId";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "setStep", "metadataFormat", 1);
    HarvestParametersEntity harvestParameters = mock(HarvestParametersEntity.class);

    AtomicReference<DatasetEntity> datasetEntityReference = new AtomicReference<>();
    when(datasetRepository.save(any())).thenAnswer(invocation -> {
      DatasetEntity datasetEntity = invocation.getArgument(0);
      datasetEntity.setDatasetId(Integer.valueOf(datasetId));
      datasetEntityReference.set(datasetEntity);
      return datasetEntity;
    });
    when(datasetRepository.findById(Integer.valueOf(datasetId))).thenAnswer(
        invocation -> Optional.ofNullable(datasetEntityReference.get()));

    when(harvestParameterService.createDatasetHarvestParameters(datasetId, oaiHarvestParametersDTO)).thenReturn(
        harvestParameters);

    ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetAndExecution(
        workflowType, datasetMetadataRequest, userId, null, oaiHarvestParametersDTO);

    assertNotNull(executionMetadata);
    assertEquals(datasetId, executionMetadata.getDatasetMetadata().getDatasetId());
    assertNull(executionMetadata.getInputMetadata().getTransformXsltEntity());
    assertEquals(harvestParameters, executionMetadata.getInputMetadata().getHarvestParametersEntity());
  }

  @Test
  void prepareDatasetExecution_createIntermediate_shouldThrowServiceExceptionOnRepositoryFailure() {
    WorkflowType workflowType = WorkflowType.OAI_HARVEST;
    String userId = "userId";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "setStep", "metadataFormat", 1);

    when(datasetRepository.save(any())).thenThrow(new RuntimeException());

    assertThrows(ServiceException.class, () ->
        datasetExecutionSetupService.prepareDatasetAndExecution(workflowType, datasetMetadataRequest, userId, null,
            oaiHarvestParametersDTO));
  }
}
