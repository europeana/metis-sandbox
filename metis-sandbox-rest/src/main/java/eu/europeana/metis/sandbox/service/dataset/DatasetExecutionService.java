package eu.europeana.metis.sandbox.service.dataset;

import static eu.europeana.metis.sandbox.common.WorkflowType.DEBIAS;
import static eu.europeana.metis.sandbox.common.WorkflowType.FILE_HARVEST_ONLY_VALIDATION;

import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import eu.europeana.metis.sandbox.dto.DatasetMetadata;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.dto.ExecutionMetadata;
import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDTO;
import eu.europeana.metis.sandbox.dto.debias.DebiasState;
import eu.europeana.metis.sandbox.dto.harvest.FileHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.HttpHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionStatus;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.common.WorkflowType;
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.engine.BatchJobExecutor;
import eu.europeana.metis.sandbox.service.util.ContentWithMaxSizeClient;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.integration.support.locks.DistributedLock;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for handling dataset execution workflows and operations.
 *
 * <p>Provides methods for creating and executing datasets with various data sources such as OAI,
 * file uploads, HTTP URLs, and debiasing operations.
 */
@Slf4j
@AllArgsConstructor
@Service
public class DatasetExecutionService {

  private static final String HARVESTING_ERROR_MESSAGE = "Error harvesting records for dataset: ";
  private final Map<String, Lock> datasetIdLocksMap = new ConcurrentHashMap<>();
  private final DatasetExecutionSetupService datasetExecutionSetupService;
  private final DeBiasStateService debiasStateService;
  private final DatasetReportService datasetReportService;
  private final LockRegistry<DistributedLock> lockRegistry;
  private final BatchJobExecutor batchJobExecutor;
  private final ContentWithMaxSizeClient contentWithMaxSizeClient;

  private static void checkFileNotFoundInProvidedUrl(String url, Exception e) {
    if (e instanceof IOException ioException
        && ioException.getMessage() != null
        && ioException.getMessage().toLowerCase(Locale.US).contains("status code 404")) {
      throw new ServiceException(HARVESTING_ERROR_MESSAGE,
          new FileNotFoundException("The provided URL does not exist or cannot be accessed: " + url));
    }
  }

  public String submitExecutionOaiSingle(String datasetId, Integer stepsize, String url, String setSpec, String metadataFormat) {
    OaiHarvestParametersDTO harvestParametersDTO =
        new OaiHarvestParametersDTO(url, normalizeSetSpec(setSpec), metadataFormat, stepsize);
    ExecutionMetadata executionMetadata =
        datasetExecutionSetupService.prepareDatasetExecutionHarvest(datasetId, harvestParametersDTO);
    return batchJobExecutor.executeStepAsync(executionMetadata, FullBatchJobType.HARVEST_OAI);
  }

  public String submitExecutionSingle(String datasetId, String sourceExecutionId, MultipartFile xsltFile, FullBatchJobType step)
      throws IOException {
    ExecutionMetadata executionMetadata =
        datasetExecutionSetupService.prepareDatasetExecution(datasetId, sourceExecutionId, xsltFile);
    return batchJobExecutor.executeStepAsync(executionMetadata, step);
  }

  /**
   * Creates a dataset based on the provided parameters and submits it for execution.
   *
   * <p>This method handles the setup and execution of a dataset workflow using OAI harvest configurations.
   * <p>After setup, it initiates the dataset processing through the batch job executor.
   *
   * @param datasetMetadataRequest the name of the dataset to be created
   * @param stepsize the number of records to process per batch
   * @param url the URL to fetch OAI-PMH records from
   * @param setSpec the set specification used for selective harvesting
   * @param metadataFormat the metadata format of the harvested records
   * @param xsltFile the XSLT file to transform the harvested records
   * @param userId the ID of the user initiating the operation
   * @return the unique dataset ID of the created and submitted dataset
   * @throws IOException if an error occurs during dataset setup or file handling
   */
  @NotNull
  public String createDatasetAndSubmitExecutionOai(DatasetMetadataRequest datasetMetadataRequest, Integer stepsize,
      String url, String setSpec, String metadataFormat, MultipartFile xsltFile, String userId) throws IOException {
    OaiHarvestParametersDTO harvestParametersDTO = new OaiHarvestParametersDTO(url, normalizeSetSpec(setSpec), metadataFormat,
        stepsize);
    ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetExecution(
        WorkflowType.OAI_HARVEST, datasetMetadataRequest, userId, xsltFile, harvestParametersDTO
    );
    batchJobExecutor.execute(executionMetadata);
    return executionMetadata.getDatasetMetadata().getDatasetId();
  }

  /**
   * Creates a dataset and handles the setup and execution of a dataset workflow using compressed FILE harvest configurations.
   *
   * <p>After setup, it initiates the dataset processing through the batch job executor.
   *
   * @param datasetMetadataRequest the name of the dataset to be created
   * @param stepsize the step size configuration for the dataset processing
   * @param compressedFile the compressed file containing dataset data
   * @param xsltFile the XSLT file for dataset transformation
   * @param userId the ID of the user initiating the operation
   * @param extension the file extension type of the compressed file
   * @return the unique identifier of the created dataset
   * @throws IOException if an I/O error occurs while processing the files
   */
  @NotNull
  public String createDatasetAndSubmitExecutionFile(DatasetMetadataRequest datasetMetadataRequest, Integer stepsize,
      MultipartFile compressedFile, MultipartFile xsltFile, String userId, CompressedFileExtension extension) throws IOException {
    FileHarvestParametersDTO fileHarvestDTO = new FileHarvestParametersDTO(compressedFile.getOriginalFilename(),
        FileType.valueOf(extension.name()),
        compressedFile.getBytes(), stepsize);
    ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetExecution(
        WorkflowType.FILE_HARVEST, datasetMetadataRequest, userId, xsltFile, fileHarvestDTO
    );
    batchJobExecutor.execute(executionMetadata);
    return executionMetadata.getDatasetMetadata().getDatasetId();
  }

  /**
   * Creates a dataset and handles the setup and execution of a dataset workflow using HTTP/URL harvest configurations.
   *
   * <p>After setup, it initiates the dataset processing through the batch job executor.
   *
   * @param datasetMetadataRequest the name of the dataset to be created
   * @param stepsize the step size for processing
   * @param url the URL of the input file to be processed
   * @param xsltFile the XSLT file for data transformation
   * @param userId the ID of the user requesting the operation
   * @param extension the file extension of the compressed input file
   * @return the unique ID of the created dataset
   */
  @NotNull
  public String createDatasetAndSubmitExecutionHttp(DatasetMetadataRequest datasetMetadataRequest, Integer stepsize,
      String url, MultipartFile xsltFile, String userId, CompressedFileExtension extension) {

    try {
      final byte[] fileContent = contentWithMaxSizeClient.download(URI.create(url));
      String filename = new URI(url).getPath();
      filename = filename.substring(filename.lastIndexOf('/') + 1);
      HttpHarvestParametersDTO harvestParametersDTO = new HttpHarvestParametersDTO(url, filename,
          FileType.valueOf(extension.name()),
          fileContent, stepsize);
      ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetExecution(
          WorkflowType.FILE_HARVEST, datasetMetadataRequest, userId, xsltFile, harvestParametersDTO
      );
      batchJobExecutor.execute(executionMetadata);
      return executionMetadata.getDatasetMetadata().getDatasetId();
    } catch (IOException | URISyntaxException e) {
      checkFileNotFoundInProvidedUrl(url, e);
      throw new ServiceException(HARVESTING_ERROR_MESSAGE, e);
    }
  }

  /**
   * Creates a dataset and handles the setup and execution of a dataset workflow using XML FILE harvest configurations.
   *
   * <p>After setup, it initiates the dataset processing through the batch job executor.
   * <p>Note: This is a blocking operation and will return when the whole workflow is complete.
   *
   * @param datasetMetadataRequest the metadata for the requested dataset
   * @param recordFile the file to be validated as part of the dataset
   * @return the ID of the created dataset
   * @throws IOException if an I/O error occurs while processing the file
   */
  public String createAndExecuteDatasetForFileValidationBlocking(DatasetMetadataRequest datasetMetadataRequest,
      MultipartFile recordFile) throws IOException {
    FileHarvestParametersDTO fileHarvestDTO = new FileHarvestParametersDTO(recordFile.getOriginalFilename(), FileType.XML,
        recordFile.getBytes(), 1);
    ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetExecution(
        FILE_HARVEST_ONLY_VALIDATION, datasetMetadataRequest, null, null, fileHarvestDTO
    );
    batchJobExecutor.executeBlocking(executionMetadata);
    return executionMetadata.getDatasetMetadata().getDatasetId();
  }

  /**
   * Creates and executes a dataset debiasing process for a given dataset ID.
   *
   * <p>Locks the process for a specific dataset ID to ensure exclusive access.
   * <p>Validates whether the process is ready for debiasing and performs necessary actions.
   * <p>Triggers the execution of the debias workflow if all conditions are met.
   *
   * @param datasetId the unique identifier of the dataset to be debiased
   * @return true if the debias workflow is successfully triggered; false otherwise
   */
  public boolean createAndExecuteDatasetForDebias(String datasetId) {

    //todo MET-6692: if the debias db creates DatasetDeBiasEntity that is unique a lock is not needed perse?
    final Lock lock = datasetIdLocksMap.computeIfAbsent(datasetId, s -> lockRegistry.obtain("debiasProcess_" + datasetId));
    try {
      lock.lock();
      log.info("DeBias process: {} lock, Locked", datasetId);
      ExecutionProgressInfoDTO executionProgressInfoDto = datasetReportService.getProgress(datasetId);
      if (executionProgressInfoDto.executionStatus() != ExecutionStatus.COMPLETED) {
        return false;
      }

      DeBiasStatusDTO debiasStatus = debiasStateService.getDeBiasStatus(datasetId);
      if (debiasStatus == null || debiasStatus.getDebiasState() != DebiasState.READY) {
        return false;
      }

      debiasStateService.remove(datasetId);

      DatasetDeBiasEntity datasetDeBiasEntity = debiasStateService.createDatasetDeBiasEntity(datasetId);
      DatasetEntity datasetEntity = datasetDeBiasEntity.getDatasetId();
      DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                       .datasetId(datasetId)
                                                       .datasetName(datasetEntity.getDatasetName())
                                                       .country(datasetEntity.getCountry())
                                                       .language(datasetEntity.getLanguage())
                                                       .workflowType(DEBIAS).build();

      ExecutionMetadata executionMetadata = ExecutionMetadata.builder()
                                                             .datasetMetadata(datasetMetadata)
                                                             .build();
      batchJobExecutor.executeDebiasWorkflow(executionMetadata);
      return true;
    } finally {
      lock.unlock();
      log.info("DeBias process: {} lock, Unlocked", datasetId);
    }

  }

  private String normalizeSetSpec(String setSpec) {
    return StringUtils.isBlank(setSpec) ? null : setSpec;
  }

  public void cancelTask(String executionId, FullBatchJobType step) throws JobExecutionNotRunningException {
    batchJobExecutor.cancelTask(executionId, step);
  }
}
