package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordException;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarningException;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionLogRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningExceptionRepository;
import eu.europeana.metis.sandbox.common.HarvestParametersConverter;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.harvest.HarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDTO;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.TierStatisticsDTO;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfoDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.sandbox.service.engine.WorkflowHelper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DatasetReportService {

  private static final String HARVESTING_IDENTIFIERS_MESSAGE = "Harvesting dataset identifiers and records.";
  private static final String PROCESSING_DATASET_MESSAGE = "A review URL will be generated when the dataset has finished processing.";
  private static final String SEPARATOR = "_";
  private static final String SUFFIX = "*";
  private final DatasetRepository datasetRepository;
  private final TransformXsltRepository transformXsltRepository;
  @Value("${sandbox.portal.publish.dataset-base-url}")
  private String portalPublishDatasetUrl;

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;
  private final ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;
  private final HarvestingParameterService harvestingParameterService;

  public DatasetReportService(
      DatasetRepository datasetRepository, ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository,
      ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository,
      TransformXsltRepository transformXsltRepository, HarvestingParameterService harvestingParameterService) {
    this.datasetRepository = datasetRepository;
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
    this.executionRecordWarningExceptionRepository = executionRecordWarningExceptionRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
    this.transformXsltRepository = transformXsltRepository;
    this.harvestingParameterService = harvestingParameterService;
  }

  public DatasetInfoDTO getDatasetInfo(String datasetId) {
    DatasetEntity datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId))
                                                   .orElseThrow(() -> new InvalidDatasetException(datasetId));
    Optional<TransformXsltEntity> xslt = transformXsltRepository.findByDatasetId(datasetId);
    HarvestParametersEntity harvestParametersEntity = harvestingParameterService.getDatasetHarvestingParameters(datasetId)
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
                         .transformedToEdmExternal(xslt.isPresent())
                         .build();
  }

  public ProgressInfoDTO getProgress(String datasetId) {
    DatasetEntity datasetEntity = datasetRepository.findByDatasetId(Integer.parseInt(datasetId))
                                                   .orElseThrow(() -> new InvalidDatasetException(datasetId));
    TransformXsltEntity transformXsltEntity = transformXsltRepository.findByDatasetId(datasetId).orElse(null);

    List<FullBatchJobType> workflowSteps = WorkflowHelper.getWorkflow(datasetEntity, transformXsltEntity);

    List<ProgressByStepDTO> progressByStepDTOS = new LinkedList<>();
    for (FullBatchJobType step : workflowSteps) {
      StepStatistics stepStatistics = getStepStatistics(datasetId, step);
      List<ErrorInfoDTO> errorInfoDTOList = getErrorInfo(datasetId, step);
      ProgressByStepDTO progressByStepDto = new ProgressByStepDTO(step, stepStatistics.totalSuccess,
          stepStatistics.totalFail, stepStatistics.totalWarning, errorInfoDTOList);
      progressByStepDTOS.add(progressByStepDto);
    }
    final long completedRecords = progressByStepDTOS.getLast().getSuccess();
    final long totalFailInWorkflow = progressByStepDTOS.stream().mapToLong(ProgressByStepDTO::getFail).sum();
    final TiersZeroInfoDTO tiersZeroInfoDTO = prepareTiersInfo(datasetId);

    return new ProgressInfoDTO(
        getPublishPortalUrl(datasetEntity, completedRecords),
        datasetEntity.getRecordsQuantity(), completedRecords + totalFailInWorkflow,
        progressByStepDTOS, datasetEntity.getRecordLimitExceeded(), "",
        null, tiersZeroInfoDTO);
  }

  private @NotNull StepStatistics getStepStatistics(
      String datasetId, FullBatchJobType fullBatchJobType) {
    String executionName = fullBatchJobType.name();
    long totalSuccess = executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        executionName);
    long totalFailure = executionRecordExceptionLogRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        executionName);
    long totalWarning = executionRecordWarningExceptionRepository.countByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(
        datasetId, executionName);
    return new StepStatistics(totalSuccess, totalFailure, totalWarning);
  }

  private List<ErrorInfoDTO> getErrorInfo(String datasetId, FullBatchJobType fullBatchJobType) {
    Map<GroupedExceptionKey, List<String>> groupedExceptions = collectGroupedExceptions(datasetId, fullBatchJobType);

    List<ErrorInfoDTO> result = new ArrayList<>();
    for (Map.Entry<GroupedExceptionKey, List<String>> entry : groupedExceptions.entrySet()) {
      List<String> sortedRecordIds = entry.getValue().stream().sorted().toList();
      result.add(new ErrorInfoDTO(entry.getKey().message(), entry.getKey().status(), sortedRecordIds
      ));
    }

    return result;
  }

  private @NotNull Map<GroupedExceptionKey, List<String>> collectGroupedExceptions(String datasetId,
      FullBatchJobType fullBatchJobType) {
    String executionName = fullBatchJobType.name();
    List<ExecutionRecordException> recordExceptionLogs =
        executionRecordExceptionLogRepository.findByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId, executionName);

    Map<GroupedExceptionKey, List<String>> groupedExceptions = new LinkedHashMap<>();
    for (ExecutionRecordException recordExceptionLog : recordExceptionLogs) {
      String recordId = recordExceptionLog.getIdentifier().getRecordId();
      GroupedExceptionKey key = new GroupedExceptionKey(Status.FAIL, recordExceptionLog.getException());
      groupedExceptions.computeIfAbsent(key, k -> new ArrayList<>()).add(recordId);
    }

    List<ExecutionRecordWarningException> warningExceptions =
        executionRecordWarningExceptionRepository.findByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(
            datasetId, executionName);

    for (ExecutionRecordWarningException warningException : warningExceptions) {
      String recordId = warningException.getExecutionRecord().getIdentifier().getRecordId();
      GroupedExceptionKey key = new GroupedExceptionKey(Status.WARN, warningException.getMessage());
      groupedExceptions.computeIfAbsent(key, k -> new ArrayList<>()).add(recordId);
    }
    return groupedExceptions;
  }

  private String getPublishPortalUrl(DatasetEntity dataset, Long completedRecords) {
    return getPortalUrl(portalPublishDatasetUrl, dataset, completedRecords);
  }

  private String getPortalUrl(String portal, DatasetEntity datasetEntity, Long completedRecords) {
    Long recordsQty = datasetEntity.getRecordsQuantity();
    if (recordsQty == null) {
      return HARVESTING_IDENTIFIERS_MESSAGE;
    }

    if (!recordsQty.equals(completedRecords)) {
      return PROCESSING_DATASET_MESSAGE;
    }

    var datasetId = datasetEntity.getDatasetId() + SEPARATOR + datasetEntity.getDatasetName() + SUFFIX;
    return portal + URLEncoder.encode(datasetId, StandardCharsets.UTF_8);
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

  public record GroupedExceptionKey(Status status, String message) {

  }
}
