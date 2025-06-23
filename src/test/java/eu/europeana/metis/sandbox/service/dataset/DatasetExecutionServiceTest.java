package eu.europeana.metis.sandbox.service.dataset;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.europeana.metis.sandbox.common.DatasetMetadata;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.common.ExecutionMetadata;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDTO;
import eu.europeana.metis.sandbox.dto.debias.DebiasState;
import eu.europeana.metis.sandbox.dto.harvest.FileHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.HttpHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionStatus;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.engine.BatchJobExecutor;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.locks.Lock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.web.multipart.MultipartFile;

@WireMockTest
@ExtendWith(MockitoExtension.class)
class DatasetExecutionServiceTest {

  @Mock
  private DatasetExecutionSetupService datasetExecutionSetupService;

  @Mock
  private DeBiasStateService debiasStateService;

  @Mock
  private DatasetReportService datasetReportService;

  @Mock
  private LockRegistry lockRegistry;

  @Mock
  private BatchJobExecutor batchJobExecutor;

  @InjectMocks
  private DatasetExecutionService datasetExecutionService;

  private static final DatasetMetadataRequest datasetMetadataRequest =
      new DatasetMetadataRequest("datasetName", Country.GREECE, Language.EL);
  private static final MultipartFile xsltFile = mock(MultipartFile.class);
  private static final String datasetId = "1";
  private static final String userId = "userId";
  private static final int steSize = 1;
  private static final String contentFilePath = "/test-path";
  private static String baseUrl;

  @BeforeEach
  void setupWireMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
    wireMockRuntimeInfo.getWireMock().register(get(contentFilePath)
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain")
            .withBody("content")));
  }

  @Test
  void createDatasetAndSubmitExecutionOai() throws IOException {
    DatasetMetadata datasetMetadata = new DatasetMetadata(datasetId, datasetMetadataRequest.datasetName(),
        datasetMetadataRequest.country(), datasetMetadataRequest.language(), WorkflowType.OAI_HARVEST);
    ExecutionMetadata executionMeta = ExecutionMetadata.builder().datasetMetadata(datasetMetadata).build();
    when(
        datasetExecutionSetupService.prepareDatasetExecution(eq(WorkflowType.OAI_HARVEST), eq(datasetMetadataRequest), eq(userId),
            eq(xsltFile), any(OaiHarvestParametersDTO.class))).thenReturn(executionMeta);

    String result = datasetExecutionService.createDatasetAndSubmitExecutionOai(datasetMetadataRequest, steSize, "url", "setSpec",
        "metadataFormat", xsltFile, userId);

    assertEquals(datasetId, result);
    verify(batchJobExecutor).execute(executionMeta);
  }

  @Test
  void createDatasetAndSubmitExecutionOai_Fail() throws IOException {
    when(
        datasetExecutionSetupService.prepareDatasetExecution(eq(WorkflowType.OAI_HARVEST), eq(datasetMetadataRequest), eq(userId),
            eq(xsltFile), any(OaiHarvestParametersDTO.class))).thenThrow(new IOException());

    assertThrows(IOException.class,
        () -> datasetExecutionService.createDatasetAndSubmitExecutionOai(datasetMetadataRequest, steSize, "url", "setSpec",
            "metadataFormat", xsltFile, userId));
    verifyNoInteractions(batchJobExecutor);
  }

  @Test
  void createDatasetAndSubmitExecutionFile() throws IOException {
    MultipartFile contentFile = mock(MultipartFile.class);
    when(contentFile.getBytes()).thenReturn("content".getBytes());
    DatasetMetadata datasetMetadata = new DatasetMetadata(datasetId, datasetMetadataRequest.datasetName(),
        datasetMetadataRequest.country(), datasetMetadataRequest.language(), WorkflowType.OAI_HARVEST);
    ExecutionMetadata executionMeta = ExecutionMetadata.builder().datasetMetadata(datasetMetadata).build();
    when(
        datasetExecutionSetupService.prepareDatasetExecution(eq(WorkflowType.FILE_HARVEST), eq(datasetMetadataRequest),
            eq(userId), eq(xsltFile), any(FileHarvestParametersDTO.class))).thenReturn(executionMeta);

    String result = datasetExecutionService.createDatasetAndSubmitExecutionFile(datasetMetadataRequest, steSize, contentFile,
        xsltFile,
        userId, CompressedFileExtension.ZIP);

    assertEquals(datasetId, result);
    verify(batchJobExecutor).execute(executionMeta);
  }

  @Test
  void createDatasetAndSubmitExecutionFile_Fail() throws IOException {
    MultipartFile contentFile = mock(MultipartFile.class);
    when(contentFile.getBytes()).thenReturn("content".getBytes());
    when(datasetExecutionSetupService.prepareDatasetExecution(eq(WorkflowType.FILE_HARVEST), eq(datasetMetadataRequest),
        eq(userId), eq(xsltFile), any(FileHarvestParametersDTO.class))).thenThrow(new IOException());

    assertThrows(IOException.class,
        () -> datasetExecutionService.createDatasetAndSubmitExecutionFile(datasetMetadataRequest, steSize, contentFile, xsltFile,
            userId, CompressedFileExtension.ZIP));
    verifyNoInteractions(batchJobExecutor);
  }

  @Test
  void createDatasetAndSubmitExecutionHttp() throws IOException {
    String url = baseUrl + contentFilePath;
    DatasetMetadata datasetMetadata = new DatasetMetadata(datasetId, datasetMetadataRequest.datasetName(),
        datasetMetadataRequest.country(), datasetMetadataRequest.language(), WorkflowType.OAI_HARVEST);
    ExecutionMetadata executionMeta = ExecutionMetadata.builder().datasetMetadata(datasetMetadata).build();
    when(
        datasetExecutionSetupService.prepareDatasetExecution(eq(WorkflowType.FILE_HARVEST), eq(datasetMetadataRequest),
            eq(userId), eq(xsltFile), any(HttpHarvestParametersDTO.class))).thenReturn(executionMeta);

    String result = datasetExecutionService.createDatasetAndSubmitExecutionHttp(datasetMetadataRequest, steSize, url, xsltFile,
        userId, CompressedFileExtension.ZIP);

    assertEquals(datasetId, result);
    verify(batchJobExecutor).execute(executionMeta);
  }

  @Test
  void createDatasetAndSubmitExecutionHttp_Fail() {
    String invalidPath = baseUrl + "/invalidPath";

    ServiceException serviceException = assertThrows(ServiceException.class,
        () -> datasetExecutionService.createDatasetAndSubmitExecutionHttp(datasetMetadataRequest, steSize, invalidPath, xsltFile,
            userId, CompressedFileExtension.ZIP));
    assertInstanceOf(FileNotFoundException.class, serviceException.getCause());

    String malformedUrl = baseUrl + "malformedUrl";
    serviceException = assertThrows(ServiceException.class,
        () -> datasetExecutionService.createDatasetAndSubmitExecutionHttp(datasetMetadataRequest, steSize, malformedUrl, xsltFile,
            userId, CompressedFileExtension.ZIP));
    assertInstanceOf(MalformedURLException.class, serviceException.getCause());

    String uriSyntaxException = "ht^tp://invalid_url";
    serviceException = assertThrows(ServiceException.class,
        () -> datasetExecutionService.createDatasetAndSubmitExecutionHttp(datasetMetadataRequest, steSize, uriSyntaxException,
            xsltFile,
            userId, CompressedFileExtension.ZIP));
    assertInstanceOf(URISyntaxException.class, serviceException.getCause());

    verifyNoInteractions(datasetExecutionSetupService);
    verifyNoInteractions(batchJobExecutor);
  }

  @Test
  void createAndExecuteDatasetForFileValidationBlocking() throws IOException {
    DatasetMetadata datasetMetadata = new DatasetMetadata(datasetId, datasetMetadataRequest.datasetName(),
        datasetMetadataRequest.country(), datasetMetadataRequest.language(), WorkflowType.OAI_HARVEST);
    ExecutionMetadata executionMeta = ExecutionMetadata.builder().datasetMetadata(datasetMetadata).build();
    when(datasetExecutionSetupService.prepareDatasetExecution(eq(WorkflowType.FILE_HARVEST_ONLY_VALIDATION),
        eq(datasetMetadataRequest), eq(null), eq(null), any(FileHarvestParametersDTO.class))).thenReturn(executionMeta);

    MultipartFile contentFile = mock(MultipartFile.class);
    when(contentFile.getBytes()).thenReturn("content".getBytes());
    String result = datasetExecutionService.createAndExecuteDatasetForFileValidationBlocking(datasetMetadataRequest, contentFile);

    assertEquals(datasetId, result);
    verify(batchJobExecutor).executeBlocking(executionMeta);
  }

  @Test
  void createAndExecuteDatasetForFileValidationBlocking_Fail() throws IOException {
    when(datasetExecutionSetupService.prepareDatasetExecution(eq(WorkflowType.FILE_HARVEST_ONLY_VALIDATION),
        eq(datasetMetadataRequest), eq(null), eq(null), any(FileHarvestParametersDTO.class))).thenThrow(new IOException());

    MultipartFile contentFile = mock(MultipartFile.class);
    when(contentFile.getBytes()).thenReturn("content".getBytes());
    assertThrows(IOException.class,
        () -> datasetExecutionService.createAndExecuteDatasetForFileValidationBlocking(datasetMetadataRequest, contentFile));

    verifyNoInteractions(batchJobExecutor);
  }

  @Test
  void createAndExecuteDatasetForDebias() {
    Lock lock = mock(Lock.class);
    when(lockRegistry.obtain(anyString())).thenReturn(lock);
    ExecutionProgressInfoDTO executionProgressInfoDTO = new ExecutionProgressInfoDTO(null, ExecutionStatus.COMPLETED, 0, 0,
        List.of(), false, null);
    when(datasetReportService.getProgress(datasetId)).thenReturn(executionProgressInfoDTO);
    DeBiasStatusDTO deBiasStatusDTO = new DeBiasStatusDTO(Integer.valueOf(datasetId), DebiasState.READY, ZonedDateTime.now(), 0L,
        0L);
    when(debiasStateService.getDeBiasStatus(datasetId)).thenReturn(deBiasStatusDTO);
    DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetName(datasetMetadataRequest.datasetName());
    datasetEntity.setCountry(datasetMetadataRequest.country());
    datasetEntity.setLanguage(datasetMetadataRequest.language());
    DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    datasetDeBiasEntity.setDatasetId(datasetEntity);
    when(debiasStateService.createDatasetDeBiasEntity(datasetId)).thenReturn(datasetDeBiasEntity);

    boolean result = datasetExecutionService.createAndExecuteDatasetForDebias(datasetId);

    assertTrue(result);
    verify(batchJobExecutor).executeDebiasWorkflow(any(ExecutionMetadata.class));
  }

  @Test
  void createAndExecuteDatasetForDebias_Fail() {
    Lock lock = mock(Lock.class);
    when(lockRegistry.obtain(anyString())).thenReturn(lock);
    ExecutionProgressInfoDTO executionProgressInfoDTO = new ExecutionProgressInfoDTO(null, ExecutionStatus.COMPLETED, 0, 0,
        List.of(), false, null);
    ExecutionProgressInfoDTO executionProgressInfoDTOInProgress = new ExecutionProgressInfoDTO(null, ExecutionStatus.IN_PROGRESS, 0, 0,
        List.of(), false, null);
    when(datasetReportService.getProgress(datasetId))
        .thenReturn(executionProgressInfoDTO).thenReturn(executionProgressInfoDTO).thenReturn(executionProgressInfoDTOInProgress);
    DeBiasStatusDTO deBiasStatusDTO = new DeBiasStatusDTO(Integer.valueOf(datasetId), DebiasState.COMPLETED, ZonedDateTime.now(), 0L,
        0L);
    when(debiasStateService.getDeBiasStatus(datasetId)).thenReturn(deBiasStatusDTO).thenReturn(null);

    assertFalse(datasetExecutionService.createAndExecuteDatasetForDebias(datasetId));
    assertFalse(datasetExecutionService.createAndExecuteDatasetForDebias(datasetId));
    assertFalse(datasetExecutionService.createAndExecuteDatasetForDebias(datasetId));
    assertFalse(datasetExecutionService.createAndExecuteDatasetForDebias(datasetId));
    verifyNoInteractions(batchJobExecutor);
  }
}