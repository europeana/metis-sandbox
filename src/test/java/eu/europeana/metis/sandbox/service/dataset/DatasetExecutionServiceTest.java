package eu.europeana.metis.sandbox.service.dataset;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static eu.europeana.metis.sandbox.entity.WorkflowType.OAI_HARVEST;
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
      DatasetMetadataRequest.builder().datasetName("datasetName").country(Country.GREECE).language(Language.EL).build();
  private static final MultipartFile xsltFile = mock(MultipartFile.class);
  private static final String DATASET_ID = "1";
  private static final String USER_ID = "userId";
  private static final int STE_SIZE = 1;
  private static final String CONTENT_FILE_PATH = "/test-path";
  private static String baseUrl;

  @BeforeEach
  void setupWireMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
    wireMockRuntimeInfo.getWireMock().register(get(CONTENT_FILE_PATH)
        .willReturn(aResponse()
            .withHeader("Content-Type", "text/plain")
            .withBody("content")));
  }

  @Test
  void createDatasetAndSubmitExecutionOai() throws IOException {
    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .datasetId(DATASET_ID)
                                                     .datasetName(datasetMetadataRequest.getDatasetName())
                                                     .country(datasetMetadataRequest.getCountry())
                                                     .language(datasetMetadataRequest.getLanguage())
                                                     .workflowType(OAI_HARVEST).build();

    ExecutionMetadata executionMeta = ExecutionMetadata.builder().datasetMetadata(datasetMetadata).build();
    when(
        datasetExecutionSetupService.prepareDatasetExecution(eq(OAI_HARVEST), eq(datasetMetadataRequest), eq(USER_ID),
            eq(xsltFile), any(OaiHarvestParametersDTO.class))).thenReturn(executionMeta);

    String result = datasetExecutionService.createDatasetAndSubmitExecutionOai(datasetMetadataRequest, STE_SIZE, "url", "setSpec",
        "metadataFormat", xsltFile, USER_ID);

    assertEquals(DATASET_ID, result);
    verify(batchJobExecutor).execute(executionMeta);
  }

  @Test
  void createDatasetAndSubmitExecutionOai_Fail() throws IOException {
    when(
        datasetExecutionSetupService.prepareDatasetExecution(eq(OAI_HARVEST), eq(datasetMetadataRequest), eq(USER_ID),
            eq(xsltFile), any(OaiHarvestParametersDTO.class))).thenThrow(new IOException());

    assertThrows(IOException.class,
        () -> datasetExecutionService.createDatasetAndSubmitExecutionOai(datasetMetadataRequest, STE_SIZE, "url", "setSpec",
            "metadataFormat", xsltFile, USER_ID));
    verifyNoInteractions(batchJobExecutor);
  }

  @Test
  void createDatasetAndSubmitExecutionFile() throws IOException {
    MultipartFile contentFile = mock(MultipartFile.class);
    when(contentFile.getBytes()).thenReturn("content".getBytes());
    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .datasetId(DATASET_ID)
                                                     .datasetName(datasetMetadataRequest.getDatasetName())
                                                     .country(datasetMetadataRequest.getCountry())
                                                     .language(datasetMetadataRequest.getLanguage())
                                                     .workflowType(OAI_HARVEST).build();
    ExecutionMetadata executionMeta = ExecutionMetadata.builder().datasetMetadata(datasetMetadata).build();
    when(
        datasetExecutionSetupService.prepareDatasetExecution(eq(WorkflowType.FILE_HARVEST), eq(datasetMetadataRequest),
            eq(USER_ID), eq(xsltFile), any(FileHarvestParametersDTO.class))).thenReturn(executionMeta);

    String result = datasetExecutionService.createDatasetAndSubmitExecutionFile(datasetMetadataRequest, STE_SIZE, contentFile,
        xsltFile,
        USER_ID, CompressedFileExtension.ZIP);

    assertEquals(DATASET_ID, result);
    verify(batchJobExecutor).execute(executionMeta);
  }

  @Test
  void createDatasetAndSubmitExecutionFile_Fail() throws IOException {
    MultipartFile contentFile = mock(MultipartFile.class);
    when(contentFile.getBytes()).thenReturn("content".getBytes());
    when(datasetExecutionSetupService.prepareDatasetExecution(eq(WorkflowType.FILE_HARVEST), eq(datasetMetadataRequest),
        eq(USER_ID), eq(xsltFile), any(FileHarvestParametersDTO.class))).thenThrow(new IOException());

    assertThrows(IOException.class,
        () -> datasetExecutionService.createDatasetAndSubmitExecutionFile(datasetMetadataRequest, STE_SIZE, contentFile, xsltFile,
            USER_ID, CompressedFileExtension.ZIP));
    verifyNoInteractions(batchJobExecutor);
  }

  @Test
  void createDatasetAndSubmitExecutionHttp() throws IOException {
    String url = baseUrl + CONTENT_FILE_PATH;
    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .datasetId(DATASET_ID)
                                                     .datasetName(datasetMetadataRequest.getDatasetName())
                                                     .country(datasetMetadataRequest.getCountry())
                                                     .language(datasetMetadataRequest.getLanguage())
                                                     .workflowType(OAI_HARVEST).build();
    ExecutionMetadata executionMeta = ExecutionMetadata.builder().datasetMetadata(datasetMetadata).build();
    when(
        datasetExecutionSetupService.prepareDatasetExecution(eq(WorkflowType.FILE_HARVEST), eq(datasetMetadataRequest),
            eq(USER_ID), eq(xsltFile), any(HttpHarvestParametersDTO.class))).thenReturn(executionMeta);

    String result = datasetExecutionService.createDatasetAndSubmitExecutionHttp(datasetMetadataRequest, STE_SIZE, url, xsltFile,
        USER_ID, CompressedFileExtension.ZIP);

    assertEquals(DATASET_ID, result);
    verify(batchJobExecutor).execute(executionMeta);
  }

  @Test
  void createDatasetAndSubmitExecutionHttp_Fail() {
    String invalidPath = baseUrl + "/invalidPath";

    ServiceException serviceException = assertThrows(ServiceException.class,
        () -> datasetExecutionService.createDatasetAndSubmitExecutionHttp(datasetMetadataRequest, STE_SIZE, invalidPath, xsltFile,
            USER_ID, CompressedFileExtension.ZIP));
    assertInstanceOf(FileNotFoundException.class, serviceException.getCause());

    String malformedUrl = baseUrl + "malformedUrl";
    serviceException = assertThrows(ServiceException.class,
        () -> datasetExecutionService.createDatasetAndSubmitExecutionHttp(datasetMetadataRequest, STE_SIZE, malformedUrl, xsltFile,
            USER_ID, CompressedFileExtension.ZIP));
    assertInstanceOf(MalformedURLException.class, serviceException.getCause());

    String uriSyntaxException = "ht^tp://invalid_url";
    serviceException = assertThrows(ServiceException.class,
        () -> datasetExecutionService.createDatasetAndSubmitExecutionHttp(datasetMetadataRequest, STE_SIZE, uriSyntaxException,
            xsltFile,
            USER_ID, CompressedFileExtension.ZIP));
    assertInstanceOf(URISyntaxException.class, serviceException.getCause());

    verifyNoInteractions(datasetExecutionSetupService);
    verifyNoInteractions(batchJobExecutor);
  }

  @Test
  void createAndExecuteDatasetForFileValidationBlocking() throws IOException {
    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .datasetId(DATASET_ID)
                                                     .datasetName(datasetMetadataRequest.getDatasetName())
                                                     .country(datasetMetadataRequest.getCountry())
                                                     .language(datasetMetadataRequest.getLanguage())
                                                     .workflowType(OAI_HARVEST).build();
    ExecutionMetadata executionMeta = ExecutionMetadata.builder().datasetMetadata(datasetMetadata).build();
    when(datasetExecutionSetupService.prepareDatasetExecution(eq(WorkflowType.FILE_HARVEST_ONLY_VALIDATION),
        eq(datasetMetadataRequest), eq(null), eq(null), any(FileHarvestParametersDTO.class))).thenReturn(executionMeta);

    MultipartFile contentFile = mock(MultipartFile.class);
    when(contentFile.getBytes()).thenReturn("content".getBytes());
    String result = datasetExecutionService.createAndExecuteDatasetForFileValidationBlocking(datasetMetadataRequest, contentFile);

    assertEquals(DATASET_ID, result);
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
    when(datasetReportService.getProgress(DATASET_ID)).thenReturn(executionProgressInfoDTO);
    DeBiasStatusDTO deBiasStatusDTO = new DeBiasStatusDTO(Integer.valueOf(DATASET_ID), DebiasState.READY, ZonedDateTime.now(), 0L,
        0L);
    when(debiasStateService.getDeBiasStatus(DATASET_ID)).thenReturn(deBiasStatusDTO);
    DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetName(datasetMetadataRequest.getDatasetName());
    datasetEntity.setCountry(datasetMetadataRequest.getCountry());
    datasetEntity.setLanguage(datasetMetadataRequest.getLanguage());
    DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    datasetDeBiasEntity.setDatasetId(datasetEntity);
    when(debiasStateService.createDatasetDeBiasEntity(DATASET_ID)).thenReturn(datasetDeBiasEntity);

    boolean result = datasetExecutionService.createAndExecuteDatasetForDebias(DATASET_ID);

    assertTrue(result);
    verify(batchJobExecutor).executeDebiasWorkflow(any(ExecutionMetadata.class));
  }

  @Test
  void createAndExecuteDatasetForDebias_Fail() {
    Lock lock = mock(Lock.class);
    when(lockRegistry.obtain(anyString())).thenReturn(lock);
    ExecutionProgressInfoDTO executionProgressInfoDTO = new ExecutionProgressInfoDTO(null, ExecutionStatus.COMPLETED, 0, 0,
        List.of(), false, null);
    ExecutionProgressInfoDTO executionProgressInfoDTOInProgress = new ExecutionProgressInfoDTO(null, ExecutionStatus.IN_PROGRESS,
        0, 0,
        List.of(), false, null);
    when(datasetReportService.getProgress(DATASET_ID))
        .thenReturn(executionProgressInfoDTO).thenReturn(executionProgressInfoDTO).thenReturn(executionProgressInfoDTOInProgress);
    DeBiasStatusDTO deBiasStatusDTO = new DeBiasStatusDTO(Integer.valueOf(DATASET_ID), DebiasState.COMPLETED, ZonedDateTime.now(),
        0L,
        0L);
    when(debiasStateService.getDeBiasStatus(DATASET_ID)).thenReturn(deBiasStatusDTO).thenReturn(null);

    assertFalse(datasetExecutionService.createAndExecuteDatasetForDebias(DATASET_ID));
    assertFalse(datasetExecutionService.createAndExecuteDatasetForDebias(DATASET_ID));
    assertFalse(datasetExecutionService.createAndExecuteDatasetForDebias(DATASET_ID));
    assertFalse(datasetExecutionService.createAndExecuteDatasetForDebias(DATASET_ID));
    verifyNoInteractions(batchJobExecutor);
  }
}