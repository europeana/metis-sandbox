package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordError;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarning;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningRepository;
import eu.europeana.metis.sandbox.common.HarvestParametersConverter;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.harvest.HarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionStatus;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressByStepDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.TierStatisticsDTO;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfoDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.DatasetRepository.DatasetIdProjection;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.sandbox.service.engine.WorkflowHelper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service class for managing dataset reports and processing statuses.
 */
@Service
public class DatasetReportService {

  private static final String HARVESTING_IDENTIFIERS_MESSAGE = "Harvesting dataset identifiers and records.";
  private static final String PROCESSING_DATASET_MESSAGE = "A review URL will be generated when the dataset has finished processing.";
  private static final String SEPARATOR = "_";
  private static final String SUFFIX = "*";
  @Value("${sandbox.portal.publish.dataset-base-url}")
  private String portalPublishDatasetUrl;
  @Value("${sandbox.dataset.max-size}")
  private int maxRecords;

  private final DatasetRepository datasetRepository;
  private final TransformXsltRepository transformXsltRepository;
  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordErrorRepository executionRecordErrorRepository;
  private final ExecutionRecordWarningRepository executionRecordWarningRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;
  private final HarvestParameterService harvestParameterService;

  /**
   * Constructor.
   *
   * @param datasetRepository repository interface for accessing dataset-related data
   * @param executionRecordRepository repository interface for managing execution records
   * @param executionRecordErrorRepository repository interface for managing execution record errors
   * @param executionRecordWarningRepository repository interface for managing execution record warnings
   * @param executionRecordTierContextRepository repository interface for managing execution record tier context
   * @param transformXsltRepository repository interface for managing XSLT transformations
   * @param harvestParameterService service for handling harvest parameters
   */
  public DatasetReportService(
      DatasetRepository datasetRepository, ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordErrorRepository executionRecordErrorRepository,
      ExecutionRecordWarningRepository executionRecordWarningRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository,
      TransformXsltRepository transformXsltRepository, HarvestParameterService harvestParameterService) {
    this.datasetRepository = datasetRepository;
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordErrorRepository = executionRecordErrorRepository;
    this.executionRecordWarningRepository = executionRecordWarningRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
    this.transformXsltRepository = transformXsltRepository;
    this.harvestParameterService = harvestParameterService;
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

  /**
   * Retrieves dataset information for the specified dataset ID.
   *
   * <p>Fetches and processes data from multiple repositories and services to assemble a complete
   * dataset information object.
   *
   * @param datasetId the unique identifier of the dataset to retrieve
   * @return a Data Transfer Object (DTO) containing the dataset's information
   */
  public DatasetInfoDTO getDatasetInfo(String datasetId) {
    DatasetEntity datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId))
                                                   .orElseThrow(() -> new InvalidDatasetException(datasetId));
    Optional<TransformXsltEntity> transformXsltEntity = transformXsltRepository.findByDatasetId(datasetId);
    HarvestParametersEntity harvestParametersEntity = harvestParameterService.getDatasetHarvestingParameters(datasetId)
                                                                             .orElseThrow();
    HarvestParametersDTO harvestParametersDTO = HarvestParametersConverter.convertToHarvestParametersDTO(harvestParametersEntity);
    return DatasetInfoDTO.builder()
                         .datasetId(datasetId)
                         .datasetName(datasetEntity.getDatasetName())
                         .createdById(datasetEntity.getCreatedById())
                         .creationDate(datasetEntity.getCreatedDate())
                         .language(datasetEntity.getLanguage())
                         .country(datasetEntity.getCountry())
                         .harvestParametersDto(harvestParametersDTO)
                         .transformedToEdmExternal(transformXsltEntity.isPresent())
                         .build();
  }

  /**
   * Retrieves the progress of the dataset workflow execution.
   *
   * <p>This method gathers the necessary information from various sources such as dataset entities,
   * workflow steps, and error details to compute the overall progress of the dataset workflow execution.
   *
   * @param datasetId the unique identifier of the dataset whose progress needs to be retrieved
   * @return an ExecutionProgressInfoDTO containing detailed progress information of the dataset execution workflow
   */
  public ExecutionProgressInfoDTO getProgress(String datasetId) {
    DatasetEntity datasetEntity = datasetRepository.findByDatasetId(Integer.parseInt(datasetId))
                                                   .orElseThrow(() -> new InvalidDatasetException(datasetId));
    TransformXsltEntity transformXsltEntity = transformXsltRepository.findByDatasetId(datasetId).orElse(null);

    List<FullBatchJobType> workflowSteps = WorkflowHelper.getWorkflow(datasetEntity, transformXsltEntity);
    List<ExecutionProgressByStepDTO> executionProgressByStepDTOS = new LinkedList<>();
    Long previousTotalRecords = null;
    boolean previousCompleted = true;
    for (FullBatchJobType step : workflowSteps) {
      StepStatistics stepStatistics = getStepStatistics(datasetId, step);
      List<ErrorInfoDTO> errorInfoDTOList = getErrorInfo(datasetId, step);

      long currentTotalRecords = stepStatistics.totalSuccess + stepStatistics.totalFail;

      long totalRecords = 0;
      if (previousTotalRecords == null) {
        // First step
        totalRecords = currentTotalRecords;
      } else if (previousCompleted) {
        // Use previous step's total if it was completed
        totalRecords = previousTotalRecords;
      }

      ExecutionProgressByStepDTO executionProgressByStepDto = new ExecutionProgressByStepDTO(
          step,
          totalRecords,
          stepStatistics.totalSuccess,
          stepStatistics.totalFail,
          stepStatistics.totalWarning,
          errorInfoDTOList
      );

      executionProgressByStepDTOS.add(executionProgressByStepDto);

      previousTotalRecords = currentTotalRecords;
      previousCompleted = (currentTotalRecords == totalRecords);
    }

    final long totalRecords = executionProgressByStepDTOS.getFirst().total();
    final long completedRecords = executionProgressByStepDTOS.getLast().success();
    final long totalFailInWorkflow = executionProgressByStepDTOS.stream().mapToLong(ExecutionProgressByStepDTO::fail).sum();
    final long totalProcessed = completedRecords + totalFailInWorkflow;
    final boolean recordLimitExceeded = totalRecords >= maxRecords;
    final TiersZeroInfoDTO tiersZeroInfoDTO = prepareTiersInfo(datasetId);

    ExecutionStatus executionStatus = computeStatus(totalRecords, totalProcessed, totalFailInWorkflow);
    String publishPortalUrl = getPublishPortalUrl(datasetEntity, totalRecords, completedRecords);

    return new ExecutionProgressInfoDTO(
        publishPortalUrl,
        executionStatus,
        totalRecords,
        totalProcessed,
        executionProgressByStepDTOS,
        recordLimitExceeded,
        tiersZeroInfoDTO
    );
  }

  private ExecutionStatus computeStatus(long totalRecords, long totalProcessed, long totalFailInWorkflow) {
    if (totalRecords > 0 && totalRecords == totalFailInWorkflow) {
      return ExecutionStatus.FAILED;
    } else if (totalRecords == 0L) {
      return ExecutionStatus.HARVESTING_IDENTIFIERS;
    } else if (totalRecords == totalProcessed) {
      return ExecutionStatus.COMPLETED;
    } else {
      return ExecutionStatus.IN_PROGRESS;
    }
  }

  private @NotNull StepStatistics getStepStatistics(
      String datasetId, FullBatchJobType fullBatchJobType) {
    String executionName = fullBatchJobType.name();
    long totalSuccess =
        executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId, executionName);
    long totalFailure =
        executionRecordErrorRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId, executionName);
    long totalWarning =
        executionRecordWarningRepository.countByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(
            datasetId, executionName);
    return new StepStatistics(totalSuccess, totalFailure, totalWarning);
  }

  private List<ErrorInfoDTO> getErrorInfo(String datasetId, FullBatchJobType fullBatchJobType) {
    Map<GroupedIssueKey, List<String>> groupedIssues = collectGroupedIssues(datasetId, fullBatchJobType);

    List<ErrorInfoDTO> result = new ArrayList<>();
    for (Map.Entry<GroupedIssueKey, List<String>> entry : groupedIssues.entrySet()) {
      List<String> sortedRecordIds = entry.getValue().stream().sorted().toList();
      result.add(new ErrorInfoDTO(entry.getKey().message(), entry.getKey().status(), sortedRecordIds
      ));
    }

    return result;
  }

  private @NotNull Map<GroupedIssueKey, List<String>> collectGroupedIssues(String datasetId, FullBatchJobType fullBatchJobType) {
    String executionName = fullBatchJobType.name();
    List<ExecutionRecordError> executionRecordErrors =
        executionRecordErrorRepository.findByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId, executionName);

    Map<GroupedIssueKey, List<String>> groupedIssues = new LinkedHashMap<>();
    for (ExecutionRecordError executionRecordError : executionRecordErrors) {
      String recordId = formatRecordId(executionRecordError.getIdentifier());
      GroupedIssueKey key = new GroupedIssueKey(Status.FAIL, executionRecordError.getException());
      groupedIssues.computeIfAbsent(key, k -> new ArrayList<>()).add(recordId);
    }

    List<ExecutionRecordWarning> executionRecordWarnings =
        executionRecordWarningRepository.findByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(
            datasetId, executionName);

    for (ExecutionRecordWarning executionRecordWarning : executionRecordWarnings) {
      String recordId = formatRecordId(executionRecordWarning.getExecutionRecord().getIdentifier());
      GroupedIssueKey key = new GroupedIssueKey(Status.WARN, executionRecordWarning.getMessage());
      groupedIssues.computeIfAbsent(key, k -> new ArrayList<>()).add(recordId);
    }
    return groupedIssues;
  }

  private static @NotNull String formatRecordId(ExecutionRecordIdentifierKey executionRecordIdentifierKey) {
    return String.format("%s | %s", executionRecordIdentifierKey.getRecordId(), executionRecordIdentifierKey.getSourceRecordId());
  }

  private String getPublishPortalUrl(DatasetEntity datasetEntity, long totalRecords, long completedRecords) {
    if (totalRecords == 0L) {
      return HARVESTING_IDENTIFIERS_MESSAGE;
    }

    if (totalRecords != completedRecords) {
      return PROCESSING_DATASET_MESSAGE;
    }

    String datasetId = datasetEntity.getDatasetId() + SEPARATOR + datasetEntity.getDatasetName() + SUFFIX;
    return portalPublishDatasetUrl + URLEncoder.encode(datasetId, StandardCharsets.UTF_8);
  }

  private TiersZeroInfoDTO prepareTiersInfo(String datasetId) {
    // get list of records with content tier 0
    List<String> listOfRecordsIdsWithContentZero =
        executionRecordTierContextRepository.findTop10ByIdentifier_DatasetIdAndContentTier(datasetId, MediaTier.T0.toString())
                                            .stream().map(ExecutionRecordTierContext::getIdentifier)
                                            .map(ExecutionRecordIdentifierKey::getRecordId).toList();

    // get list of records with metadata tier 0
    List<String> listOfRecordsIdsWithMetadataZero =
        executionRecordTierContextRepository.findTop10ByIdentifier_DatasetIdAndMetadataTier(datasetId, MetadataTier.T0.toString())
                                            .stream()
                                            .map(ExecutionRecordTierContext::getIdentifier)
                                            .map(ExecutionRecordIdentifierKey::getRecordId)
                                            .toList();

    // encapsulate values into TierStatistics. Cut list of record ids into limit number
    TierStatisticsDTO contentTierInfo = listOfRecordsIdsWithContentZero.isEmpty() ? null :
        new TierStatisticsDTO(
            Math.toIntExact(executionRecordTierContextRepository.countByIdentifier_DatasetIdAndContentTier(datasetId,
                MediaTier.T0.toString())), listOfRecordsIdsWithContentZero);

    // encapsulate values into TierStatistics. Cut list of record ids into limit number
    TierStatisticsDTO metadataTierInfo = listOfRecordsIdsWithMetadataZero.isEmpty() ? null :
        new TierStatisticsDTO(
            Math.toIntExact(executionRecordTierContextRepository.countByIdentifier_DatasetIdAndMetadataTier(datasetId,
                MetadataTier.T0.toString())),
            listOfRecordsIdsWithMetadataZero);

    // encapsulate values into TiersZeroInfo
    return contentTierInfo == null && metadataTierInfo == null ? null :
        new TiersZeroInfoDTO(contentTierInfo, metadataTierInfo);
  }

  private record StepStatistics(long totalSuccess, long totalFail, long totalWarning) {

  }

  private record GroupedIssueKey(Status status, String message) {

  }
}
