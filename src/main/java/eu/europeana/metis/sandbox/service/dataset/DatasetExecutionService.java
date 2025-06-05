package eu.europeana.metis.sandbox.service.dataset;

import static eu.europeana.metis.sandbox.entity.WorkflowType.DEBIAS;
import static eu.europeana.metis.sandbox.entity.WorkflowType.FILE_HARVEST_ONLY_VALIDATION;

import eu.europeana.metis.sandbox.common.DatasetMetadata;
import eu.europeana.metis.sandbox.common.ExecutionMetadata;
import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDTO;
import eu.europeana.metis.sandbox.dto.harvest.FileHarvestDTO;
import eu.europeana.metis.sandbox.dto.harvest.HttpHarvestDTO;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestDTO;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDTO.Status;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.engine.BatchJobExecutor;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DatasetExecutionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String HARVESTING_ERROR_MESSAGE = "Error harvesting records for dataset: ";
  private final Map<String, Lock> datasetIdLocksMap = new ConcurrentHashMap<>();
  private final DatasetExecutionSetupService datasetExecutionSetupService;
  private final DeBiasStateService debiasStateService;
  private final DatasetReportService datasetReportService;
  private final LockRegistry lockRegistry;
  private final BatchJobExecutor batchJobExecutor;

  public DatasetExecutionService(DatasetExecutionSetupService datasetExecutionSetupService, DeBiasStateService debiasStateService,
      DatasetReportService datasetReportService, LockRegistry lockRegistry, BatchJobExecutor batchJobExecutor) {
    this.datasetExecutionSetupService = datasetExecutionSetupService;
    this.debiasStateService = debiasStateService;
    this.datasetReportService = datasetReportService;
    this.lockRegistry = lockRegistry;
    this.batchJobExecutor = batchJobExecutor;
  }

  @NotNull
  public String createDatasetAndSubmitExecution(String datasetName, Country country, Language language, Integer stepsize,
      String url, String setSpec, String metadataFormat, MultipartFile xsltFile, String userId) throws IOException {
    OaiHarvestDTO harvestParametersDTO = new OaiHarvestDTO(url, normalizeSetSpec(setSpec), metadataFormat);
    ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetExecution(
        WorkflowType.OAI_HARVEST, datasetName, country, language, stepsize, userId, xsltFile, harvestParametersDTO
    );
    batchJobExecutor.execute(executionMetadata);
    return executionMetadata.getDatasetMetadata().getDatasetId();
  }

  @NotNull
  public String createDatasetAndSubmitExecution(String datasetName, Country country, Language language, Integer stepsize,
      MultipartFile compressedFile, MultipartFile xsltFile, String userId, CompressedFileExtension extension) throws IOException {
    FileHarvestDTO fileHarvestDTO = new FileHarvestDTO(compressedFile.getOriginalFilename(), FileType.valueOf(extension.name()),
        compressedFile.getBytes());
    ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetExecution(
        WorkflowType.FILE_HARVEST, datasetName, country, language, stepsize, userId, xsltFile, fileHarvestDTO
    );
    batchJobExecutor.execute(executionMetadata);
    return executionMetadata.getDatasetMetadata().getDatasetId();
  }

  @NotNull
  public String createDatasetAndSubmitExecution(String datasetName, Country country, Language language, Integer stepsize,
      String url, MultipartFile xsltFile, String userId, CompressedFileExtension extension) throws IOException {
    String filename = Path.of(url).getFileName().toString();
    try (InputStream inputStream = new URI(url).toURL().openStream()) {
      HttpHarvestDTO harvestParametersDTO = new HttpHarvestDTO(url, filename, FileType.valueOf(extension.name()),
          inputStream.readAllBytes());
      ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetExecution(
          WorkflowType.FILE_HARVEST, datasetName, country, language, stepsize, userId, xsltFile, harvestParametersDTO
      );
      batchJobExecutor.execute(executionMetadata);
      return executionMetadata.getDatasetMetadata().getDatasetId();
    } catch (UnknownHostException e) {
      throw new ServiceException(HARVESTING_ERROR_MESSAGE + " - Unknown host", e);
    } catch (IOException | URISyntaxException e) {
      throw new ServiceException(HARVESTING_ERROR_MESSAGE, e);
    }
  }

  public String createAndExecuteDatasetForFileValidationBlocking(String datasetName,
      MultipartFile recordFile, Country country, Language language) throws IOException {
    FileHarvestDTO fileHarvestDTO = new FileHarvestDTO(recordFile.getOriginalFilename(), FileType.XML, recordFile.getBytes());
    ExecutionMetadata executionMetadata = datasetExecutionSetupService.prepareDatasetExecution(
        FILE_HARVEST_ONLY_VALIDATION, datasetName, country, language, 1, null, null, fileHarvestDTO
    );
    batchJobExecutor.executeBlocking(executionMetadata);
    return executionMetadata.getDatasetMetadata().getDatasetId();
  }

  public boolean createAndExecuteDatasetForDebias(String datasetId) {

    //todo: if the debias db creates DatasetDeBiasEntity that is unique a lock is not needed perse.
    final Lock lock = datasetIdLocksMap.computeIfAbsent(datasetId, s -> lockRegistry.obtain("debiasProcess_" + datasetId));
    try {
      lock.lock();
      LOGGER.info("DeBias process: {} lock, Locked", datasetId);
      ProgressInfoDTO progressInfoDto = datasetReportService.getProgress(datasetId);

      if (progressInfoDto.getStatus().equals(Status.COMPLETED) &&
          "READY".equals(Optional.ofNullable(debiasStateService.getDeBiasStatus(String.valueOf(datasetId)))
                                 .map(DeBiasStatusDTO::getState)
                                 .orElse(""))) {
        debiasStateService.remove(datasetId);

        DatasetDeBiasEntity datasetDeBiasEntity = debiasStateService.createDatasetDeBiasEntity(datasetId);
        DatasetEntity datasetEntity = datasetDeBiasEntity.getDatasetId();
        DatasetMetadata datasetMetadata = new DatasetMetadata(datasetId, datasetEntity.getDatasetName(),
            datasetEntity.getCountry(), datasetEntity.getLanguage(), 1, DEBIAS);

        ExecutionMetadata executionMetadata = ExecutionMetadata.builder()
                                                               .datasetMetadata(datasetMetadata)
                                                               .build();
        batchJobExecutor.executeDebiasWorkflow(executionMetadata);
        return true;
      } else {
        return false;
      }
    } finally {
      lock.unlock();
      LOGGER.info("DeBias process: {} lock, Unlocked", datasetId);
    }

  }

  private String normalizeSetSpec(String setSpec) {
    return StringUtils.isBlank(setSpec) ? null : setSpec;
  }

}
