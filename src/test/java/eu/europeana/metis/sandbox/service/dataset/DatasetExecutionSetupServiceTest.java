package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.ExecutionMetadata;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestParametersDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

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

  @Test
  void prepareDatasetExecution() throws IOException {
    String datasetId = "1";
    String datasetName = "DatasetName";
    WorkflowType workflowType = WorkflowType.OAI_HARVEST;
    Country country = Country.GREECE;
    Language language = Language.EL;
    String userId = "userId";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "setStep", "metadataFormat", 1);
    HarvestParametersEntity harvestParameters = mock(HarvestParametersEntity.class);
    when(datasetRepository.save(any())).thenAnswer(invocation -> {
      DatasetEntity datasetEntity = invocation.getArgument(0);
      datasetEntity.setDatasetId(Integer.valueOf(datasetId));
      return datasetEntity;
    });
    MultipartFile xsltFile = mock(MultipartFile.class);
    String xsltContent = "<xsl:stylesheet></xsl:stylesheet>";
    when(xsltFile.getBytes()).thenReturn(xsltContent.getBytes(StandardCharsets.UTF_8));

    when(harvestParameterService.createDatasetHarvestParameters(datasetId, oaiHarvestParametersDTO)).thenReturn(
        harvestParameters);

    ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetExecution(
        workflowType, datasetName, country, language, userId, xsltFile, oaiHarvestParametersDTO);

    assertNotNull(executionMetadata);
    assertEquals(datasetId, executionMetadata.getDatasetMetadata().getDatasetId());
    assertNotNull(executionMetadata.getInputMetadata().getTransformXsltEntity());
    assertEquals(harvestParameters, executionMetadata.getInputMetadata().getHarvestParametersEntity());
    verify(transformXsltRepository).save(any(TransformXsltEntity.class));
  }

  @Test
  void prepareDatasetExecution_withoutXslt() throws IOException {
    String datasetId = "1";
    String datasetName = "DatasetName";
    WorkflowType workflowType = WorkflowType.OAI_HARVEST;
    Country country = Country.GREECE;
    Language language = Language.EL;
    String userId = "userId";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "setStep", "metadataFormat", 1);
    HarvestParametersEntity harvestParameters = mock(HarvestParametersEntity.class);

    when(datasetRepository.save(any())).thenAnswer(invocation -> {
      DatasetEntity datasetEntity = invocation.getArgument(0);
      datasetEntity.setDatasetId(Integer.valueOf(datasetId));
      return datasetEntity;
    });

    when(harvestParameterService.createDatasetHarvestParameters(datasetId, oaiHarvestParametersDTO)).thenReturn(
        harvestParameters);

    ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetExecution(
        workflowType, datasetName, country, language, userId, null, oaiHarvestParametersDTO);

    assertNotNull(executionMetadata);
    assertEquals(datasetId, executionMetadata.getDatasetMetadata().getDatasetId());
    assertNull(executionMetadata.getInputMetadata().getTransformXsltEntity());
    assertEquals(harvestParameters, executionMetadata.getInputMetadata().getHarvestParametersEntity());
  }

  @Test
  void prepareDatasetExecution_createDataset_shouldThrowServiceExceptionOnRepositoryFailure() {
    String datasetName = "DatasetName";
    WorkflowType workflowType = WorkflowType.OAI_HARVEST;
    Country country = Country.GREECE;
    Language language = Language.EL;
    String userId = "userId";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "setStep", "metadataFormat", 1);

    when(datasetRepository.save(any())).thenThrow(new RuntimeException());

    assertThrows(ServiceException.class, () ->
        datasetExecutionSetupService.prepareDatasetExecution(workflowType, datasetName, country, language, userId, null,
            oaiHarvestParametersDTO));
  }

  @Test
  void prepareDatasetExecution_createDataset_BlankValues() {
    String datasetName = "DatasetName";
    WorkflowType workflowType = WorkflowType.OAI_HARVEST;
    Country country = Country.GREECE;
    Language language = Language.EL;
    String userId = "userId";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "setStep", "metadataFormat", 1);

    assertThrows(NullPointerException.class, () ->
        datasetExecutionSetupService.prepareDatasetExecution(workflowType, null, country, language, userId, null,
            oaiHarvestParametersDTO));
    assertThrows(IllegalArgumentException.class, () ->
        datasetExecutionSetupService.prepareDatasetExecution(workflowType, "", country, language, userId, null,
            oaiHarvestParametersDTO));
    assertThrows(NullPointerException.class, () ->
        datasetExecutionSetupService.prepareDatasetExecution(workflowType, datasetName, null, language, userId, null,
            oaiHarvestParametersDTO));
    assertThrows(NullPointerException.class, () ->
        datasetExecutionSetupService.prepareDatasetExecution(workflowType, datasetName, country, null, userId, null,
            oaiHarvestParametersDTO));
  }

  @Test
  void prepareDatasetExecution_saveXslt_ThrowIOException() throws IOException {
    String datasetId = "1";
    String datasetName = "DatasetName";
    WorkflowType workflowType = WorkflowType.OAI_HARVEST;
    Country country = Country.GREECE;
    Language language = Language.EL;
    String userId = "userId";
    OaiHarvestParametersDTO oaiHarvestParametersDTO = new OaiHarvestParametersDTO("url", "setStep", "metadataFormat", 1);

    when(datasetRepository.save(any())).thenAnswer(invocation -> {
      DatasetEntity datasetEntity = invocation.getArgument(0);
      datasetEntity.setDatasetId(Integer.valueOf(datasetId));
      return datasetEntity;
    });

    MultipartFile xsltFile = mock(MultipartFile.class);
    when(xsltFile.getBytes()).thenThrow(new IOException());

    assertThrows(IOException.class, () ->
        datasetExecutionSetupService.prepareDatasetExecution(workflowType, datasetName, country, language, userId, xsltFile,
            oaiHarvestParametersDTO));
  }
}
