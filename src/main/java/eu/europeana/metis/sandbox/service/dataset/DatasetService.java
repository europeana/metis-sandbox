package eu.europeana.metis.sandbox.service.dataset;

import static eu.europeana.metis.sandbox.entity.WorkflowType.DEBIAS;
import static eu.europeana.metis.sandbox.entity.WorkflowType.FILE_HARVEST_ONLY_VALIDATION;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.DatasetMetadata;
import eu.europeana.metis.sandbox.common.ExecutionMetadata;
import eu.europeana.metis.sandbox.common.InputMetadata;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.exception.XsltProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.FileHarvestDTO;
import eu.europeana.metis.sandbox.dto.HarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.HttpHarvestDTO;
import eu.europeana.metis.sandbox.dto.OAIPmhHarvestDTO;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDTO;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDTO.Status;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.DatasetRepository.DatasetIdProjection;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.engine.BatchJobExecutor;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
public class DatasetService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String HARVESTING_ERROR_MESSAGE = "Error harvesting records for dataset: ";

  private final DatasetRepository datasetRepository;
  private final TransformXsltRepository transformXsltRepository;
  private final HarvestingParameterService harvestingParameterService;
  private final DeBiasStateService debiasStateService;
  private final DatasetReportService datasetReportService;
  private final LockRegistry lockRegistry;
  private final BatchJobExecutor batchJobExecutor;
  private final Map<String, Lock> datasetIdLocksMap = new ConcurrentHashMap<>();

  public DatasetService(DatasetRepository datasetRepository, TransformXsltRepository transformXsltRepository,
      HarvestingParameterService harvestingParameterService,
      DeBiasStateService debiasStateService, DatasetReportService datasetReportService, LockRegistry lockRegistry,
      BatchJobExecutor batchJobExecutor) {
    this.datasetRepository = datasetRepository;
    this.transformXsltRepository = transformXsltRepository;
    this.harvestingParameterService = harvestingParameterService;
    this.debiasStateService = debiasStateService;
    this.datasetReportService = datasetReportService;
    this.lockRegistry = lockRegistry;
    this.batchJobExecutor = batchJobExecutor;
  }

  @NotNull
  public String createDatasetAndSubmitExecution(String datasetName, Country country, Language language, Integer stepsize,
      String url, String setSpec, String metadataFormat, MultipartFile xsltFile, String userId) throws IOException {
    String datasetId = createDataset(WorkflowType.OAI_HARVEST, datasetName, userId, country, language);
    TransformXsltEntity transformXsltEntity = new TransformXsltEntity(datasetId,
        new String(xsltFile.getBytes(), StandardCharsets.UTF_8), "EXTERNAL");
    transformXsltRepository.save(transformXsltEntity);

    DatasetMetadata datasetMetadata = buildDatasetMetadata(datasetId, datasetName, country, language, stepsize,
        WorkflowType.OAI_HARVEST);
    OAIPmhHarvestDTO harvestParametersDTO = new OAIPmhHarvestDTO(url, normalizeSetSpec(setSpec), metadataFormat);
    harvestingParameterService.createDatasetHarvestParameters(datasetId, harvestParametersDTO);

    InputMetadata inputMetadata = new InputMetadata(url, normalizeSetSpec(setSpec), metadataFormat, stepsize,
        transformXsltEntity);
    submitToExecutor(datasetMetadata, inputMetadata);
    return datasetId;
  }

  @NotNull
  public String createDatasetAndSubmitExecution(String datasetName, Country country, Language language, Integer stepsize,
      MultipartFile compressedFile, MultipartFile xsltFile, String userId, CompressedFileExtension extension) throws IOException {
    String datasetId = createDataset(WorkflowType.FILE_HARVEST, datasetName, userId, country, language);
    TransformXsltEntity transformXsltEntity = new TransformXsltEntity(datasetId,
        new String(xsltFile.getBytes(), StandardCharsets.UTF_8), "EXTERNAL");
    transformXsltRepository.save(transformXsltEntity);

    DatasetMetadata datasetMetadata = buildDatasetMetadata(datasetId, datasetName, country, language, stepsize,
        WorkflowType.FILE_HARVEST);
    FileHarvestDTO fileHarvestDTO = new FileHarvestDTO(compressedFile.getOriginalFilename(), extension.name(),
        compressedFile.getBytes());
    HarvestParametersEntity datasetHarvestParameters =
        harvestingParameterService.createDatasetHarvestParameters(datasetId, fileHarvestDTO);

    InputMetadata inputMetadata = new InputMetadata(datasetHarvestParameters, extension, stepsize, transformXsltEntity);

    submitToExecutor(datasetMetadata, inputMetadata);
    return datasetId;
  }

  @NotNull
  public String createDatasetAndSubmitExecution(String datasetName, Country country, Language language, Integer stepsize,
      String url, MultipartFile xsltFile, String userId, CompressedFileExtension extension) throws IOException {
    String datasetId = createDataset(WorkflowType.FILE_HARVEST, datasetName, userId, country, language);
    TransformXsltEntity transformXsltEntity = new TransformXsltEntity(datasetId,
        new String(xsltFile.getBytes(), StandardCharsets.UTF_8), "EXTERNAL");
    transformXsltRepository.save(transformXsltEntity);

    DatasetMetadata datasetMetadata = buildDatasetMetadata(datasetId, datasetName, country, language, stepsize,
        WorkflowType.FILE_HARVEST);

    try (InputStream inputStream = new URI(url).toURL().openStream()) {
      HttpHarvestDTO harvestParametersDTO = new HttpHarvestDTO(url, extension.name(), inputStream.readAllBytes());
      HarvestParametersEntity datasetHarvestParameters =
          harvestingParameterService.createDatasetHarvestParameters(datasetId, harvestParametersDTO);
      InputMetadata inputMetadata = new InputMetadata(datasetHarvestParameters, extension, stepsize, transformXsltEntity);
      submitToExecutor(datasetMetadata, inputMetadata);
    } catch (UnknownHostException e) {
      throw new ServiceException(HARVESTING_ERROR_MESSAGE + datasetId + " - Unknown host", e);
    } catch (IOException | URISyntaxException e) {
      throw new ServiceException(HARVESTING_ERROR_MESSAGE + datasetId, e);
    }
    return datasetId;
  }

  public String createAndExecuteDatasetForFileValidationBlocking(String datasetName,
      MultipartFile recordFile, Country country, Language language) throws IOException {
    String datasetId = createDataset(FILE_HARVEST_ONLY_VALIDATION, datasetName, null, country, language);

    FileHarvestDTO fileHarvestDTO = new FileHarvestDTO(recordFile.getOriginalFilename(), "xml", recordFile.getBytes());
    HarvestParametersEntity datasetHarvestParameters =
        harvestingParameterService.createDatasetHarvestParameters(datasetId, fileHarvestDTO);
    DatasetMetadata datasetMetadata = buildDatasetMetadata(datasetId, datasetName, country, language, 1,
        FILE_HARVEST_ONLY_VALIDATION);

    InputMetadata inputMetadata = new InputMetadata(datasetHarvestParameters);
    submitToExecutorBlocking(datasetMetadata, inputMetadata);
    return datasetId;
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
        DatasetMetadata datasetMetadata = buildDatasetMetadata(datasetId, datasetEntity.getDatasetName(),
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

  private String readXslt(MultipartFile xsltFile) {
    if (xsltFile != null && !xsltFile.isEmpty()) {
      try {
        if (!StringUtils.containsIgnoreCase(requireNonNull(xsltFile.getContentType()), "xml")) {
          throw new IllegalArgumentException("XSLT file must be XML.");
        }
        return new String(xsltFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new XsltProcessingException("Failed to process XSLT file.", e);
      }
    }
    return StringUtils.EMPTY;
  }

  private DatasetMetadata buildDatasetMetadata(String id, String name, Country country, Language lang, Integer stepsize,
      WorkflowType type) {
    return DatasetMetadata.builder()
                          .datasetId(id)
                          .datasetName(name)
                          .country(country)
                          .language(lang)
                          .stepSize(stepsize)
                          .workflowType(type)
                          .build();
  }


  private void submitToExecutor(DatasetMetadata metadata, InputMetadata inputMetadata) {
    batchJobExecutor.execute(ExecutionMetadata.builder()
                                              .datasetMetadata(metadata)
                                              .inputMetadata(inputMetadata)
                                              .build());
  }

  private void submitToExecutorBlocking(DatasetMetadata metadata, InputMetadata inputMetadata) {
    batchJobExecutor.executeBlocking(ExecutionMetadata.builder()
                                                      .datasetMetadata(metadata)
                                                      .inputMetadata(inputMetadata)
                                                      .build());
  }

  private String normalizeSetSpec(String setSpec) {
    return StringUtils.isBlank(setSpec) ? null : setSpec;
  }

  public String createDataset(WorkflowType workflowType, String name, String userId, Country country,
      Language language) {
    requireNonNull(name, "Dataset name is required");
    requireNonNull(country, "Country is required");
    requireNonNull(language, "Language is required");

    DatasetEntity datasetEntity = new DatasetEntity(workflowType, name, userId, null, language, country, false);

    try {
      return String.valueOf(datasetRepository.save(datasetEntity).getDatasetId());
    } catch (RuntimeException e) {
      throw new ServiceException(format("Failed to create dataset [%s]", name), e);
    }
  }

  public List<String> findDatasetIdsByCreatedBefore(int days) {
    ZonedDateTime retentionDate = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(days);

    try {
      return datasetRepository.findByCreatedDateBefore(retentionDate).stream()
                              .map(DatasetIdProjection::getDatasetId)
                              .map(Object::toString)
                              .toList();
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error getting datasets older than %s days. ", days), e);
    }
  }

  public void setRecordLimitExceeded(String datasetId) {
    datasetRepository.setRecordLimitExceeded(Integer.parseInt(datasetId));
  }

  public DatasetInfoDTO getDatasetInfo(String datasetId) {
    DatasetEntity datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId))
                                                   .orElseThrow(() -> new InvalidDatasetException(datasetId));
    Optional<TransformXsltEntity> xslt = transformXsltRepository.findByDatasetId(datasetId);
    return new DatasetInfoDTO.Builder()
        .datasetId(datasetId)
        .datasetName(datasetEntity.getDatasetName())
        .createdById(datasetEntity.getCreatedById())
        .creationDate(datasetEntity.getCreatedDate())
        .language(datasetEntity.getLanguage())
        .country(datasetEntity.getCountry())
        .harvestingParametricDto(getHarvestingParameterDto(datasetId))
        .transformedToEdmExternal(xslt.isPresent())
        .build();
  }

  private HarvestParametersDTO getHarvestingParameterDto(String datasetId) {
    HarvestParametersEntity harvestParametersEntity = harvestingParameterService.getDatasetHarvestingParameters(datasetId);

    return switch (harvestParametersEntity.getHarvestProtocol()) {
      case FILE -> new FileHarvestDTO(harvestParametersEntity.getFileName(), harvestParametersEntity.getFileType(),
          harvestParametersEntity.getFileContent());
      case HTTP -> new HttpHarvestDTO(harvestParametersEntity.getUrl(), harvestParametersEntity.getFileType(),
          harvestParametersEntity.getFileContent());
      case OAI_PMH -> new OAIPmhHarvestDTO(harvestParametersEntity.getUrl(), harvestParametersEntity.getSetSpec(),
          harvestParametersEntity.getMetadataFormat());
    };
  }

  public void remove(String datasetId) {
    try {
      datasetRepository.deleteById(Integer.valueOf(datasetId));
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error removing dataset id: [%s]. ", datasetId), e);
    }
  }
}
