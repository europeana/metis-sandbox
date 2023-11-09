package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.aggregation.StepStatistic;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.HarvestingParametricDto;
import eu.europeana.metis.sandbox.dto.FileHarvestingDto;
import eu.europeana.metis.sandbox.dto.HttpHarvestingDto;
import eu.europeana.metis.sandbox.dto.OAIPmhHarvestingDto;
import eu.europeana.metis.sandbox.dto.report.DatasetLogDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.TierStatistics;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfo;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.HarvestingParameterEntity;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.projection.ErrorLogView;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;

@Service
class DatasetReportServiceImpl implements DatasetReportService {

    private static final int FIRST = 0;
    private static final String EMPTY_DATASET_MESSAGE = "Dataset is empty.";
    private static final String HARVESTING_IDENTIFIERS_MESSAGE = "Harvesting dataset identifiers and records.";
    private static final String PROCESSING_DATASET_MESSAGE = "A review URL will be generated when the dataset has finished processing.";
    private static final String SEPARATOR = "_";
    private static final String SUFFIX = "*";
    private final DatasetRepository datasetRepository;
    private final DatasetLogService datasetLogService;
    private final RecordLogRepository recordLogRepository;
    private final RecordErrorLogRepository errorLogRepository;
    private final RecordRepository recordRepository;
    @Value("${sandbox.portal.publish.dataset-base-url}")
    private String portalPublishDatasetUrl;

    public DatasetReportServiceImpl(
            DatasetRepository datasetRepository,
            DatasetLogService datasetLogService,
            RecordLogRepository recordLogRepository,
            RecordErrorLogRepository errorLogRepository,
            RecordRepository recordRepository,
            HarvestingParameterService harvestingParameterService) {
        this.datasetRepository = datasetRepository;
        this.datasetLogService = datasetLogService;
        this.recordLogRepository = recordLogRepository;
        this.errorLogRepository = errorLogRepository;
        this.recordRepository = recordRepository;
    }

    private static Stream<DatasetLogDto> getErrors(List<DatasetLogDto> datasetLogs) {
        return datasetLogs.stream().filter(log -> log.getType() == eu.europeana.metis.sandbox.common.Status.FAIL);
    }

    private static String createMessageRecordError(RecordEntity recordEntity) {
        return Stream.of(recordEntity.getEuropeanaId(), recordEntity.getProviderId()).filter(
                Objects::nonNull).filter(id -> !id.isBlank()).collect(Collectors.joining(" | "));
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressInfoDto getReport(String datasetId) {
        requireNonNull(datasetId, "Dataset id must not be null");

        // search for dataset
        DatasetEntity dataset = getDataset(datasetId);

        //Create DatasetInfoDto from DatasetEntity
        DatasetInfoDto datasetInfoDto = new DatasetInfoDto(datasetId, dataset.getDatasetName(), dataset.getCreatedDate(),
                dataset.getLanguage(), dataset.getCountry(), dataset.getRecordLimitExceeded(),
                StringUtils.isNotBlank(dataset.getXsltEdmExternalContent()));

        // pull records and errors data for the dataset
        List<StepStatistic> stepStatistics;
        List<ErrorLogView> errorsLog;
        try {
            stepStatistics = recordLogRepository.getStepStatistics(datasetId);
            errorsLog = errorLogRepository.getByRecordIdDatasetId(datasetId);
        } catch (RuntimeException exception) {
            throw new ServiceException(format("Failed to get report for dataset id: [%s]. ", datasetId),
                    exception);
        }

        // get qty of records completely processed
        long completedRecords = getCompletedRecords(stepStatistics);

        List<DatasetLogDto> datasetLogs = datasetLogService.getAllLogs(datasetId);
        if (stepStatistics.isEmpty() || stepStatistics.stream().allMatch(step -> step.getStatus().equals(Status.FAIL))
                || getErrors(datasetLogs).findAny().isPresent()) {
            return new ProgressInfoDto(getPublishPortalUrl(dataset, 0L),
                    dataset.getRecordsQuantity(), 0L, List.of(),
                    datasetInfoDto, getErrorMessage(datasetLogs, dataset.getRecordsQuantity()),
                    datasetLogs,
                    null);
        }

        // get records processed by step
        Map<Step, Map<Status, Long>> recordsProcessedByStep = getStatisticsByStep(
                stepStatistics);

        // get errors by step
        Map<Step, Map<Status, Map<String, List<ErrorLogView>>>> recordErrorsByStep = getRecordErrorsByStep(
                errorsLog);

        // collect steps processing information
        List<ProgressByStepDto> stepsInfo = new LinkedList<>();
        recordsProcessedByStep.forEach((step, statusMap) -> addStepInfo(stepsInfo, statusMap, step,
                recordErrorsByStep));

        TiersZeroInfo tiersZeroInfo = prepareTiersInfo(datasetId);

        return new ProgressInfoDto(
                getPublishPortalUrl(dataset, completedRecords),
                dataset.getRecordsQuantity(), completedRecords,
                stepsInfo, datasetInfoDto, getErrorMessage(datasetLogs, dataset.getRecordsQuantity()),
                datasetLogs, tiersZeroInfo);
    }

    private String getPublishPortalUrl(DatasetEntity dataset, Long completedRecords) {
        return getPortalUrl(portalPublishDatasetUrl, dataset, completedRecords);
    }

    private String getPortalUrl(String portal, DatasetEntity dataset, Long completedRecords) {
        Long recordsQty = dataset.getRecordsQuantity();
        if (recordsQty == null) {
            return HARVESTING_IDENTIFIERS_MESSAGE;
        }

        if (!recordsQty.equals(completedRecords)) {
            return PROCESSING_DATASET_MESSAGE;
        }

        var datasetId = dataset.getDatasetId() + SEPARATOR + dataset.getDatasetName() + SUFFIX;
        return portal + URLEncoder.encode(datasetId, StandardCharsets.UTF_8);
    }

    private String getErrorMessage(List<DatasetLogDto> datasetLogs, Long recordsQuantity) {
        if (getErrors(datasetLogs).findAny().isPresent()) {
            return getErrors(datasetLogs).map(DatasetLogDto::getMessage).collect(Collectors.joining(","));
        }

        if (recordsQuantity == null) {
            return "";
        } else if (recordsQuantity == 0) {
            return EMPTY_DATASET_MESSAGE;
        } else {
            return "";
        }
    }

    private DatasetEntity getDataset(String datasetId) {
        Optional<DatasetEntity> optionalDataset;

        try {
            optionalDataset = datasetRepository.findById(Integer.valueOf(datasetId));
        } catch (RuntimeException exception) {
            throw new ServiceException(format("Failed to get dataset with id: [%s]. ", datasetId),
                    exception);
        }

        return optionalDataset.orElseThrow(() -> new InvalidDatasetException(datasetId));
    }

    private Long getCompletedRecords(List<StepStatistic> stepStatistics) {
        return stepStatistics.stream()
                .filter(current -> current.getStep() == Step.CLOSE || current.getStatus() == Status.FAIL)
                .mapToLong(StepStatistic::getCount)
                .sum();
    }

    private Map<Step, Map<Status, Long>> getStatisticsByStep(
            List<StepStatistic> stepStatistics) {
        return stepStatistics.stream()
                .filter(x -> x.getStep() != Step.CLOSE)
                .sorted(Comparator.comparingInt(stepStatistic -> stepStatistic.getStep().precedence()))
                .collect(groupingBy(StepStatistic::getStep, LinkedHashMap::new,
                        groupingBy(StepStatistic::getStatus,
                                reducing(0L, StepStatistic::getCount, Long::sum))));
    }

    private Map<Step, Map<Status, Map<String, List<ErrorLogView>>>> getRecordErrorsByStep(
            List<ErrorLogView> errorsLog) {
        if (errorsLog.isEmpty()) {
            return Map.of();
        }
        return errorsLog
                .stream()
                .sorted(Comparator.comparingInt((ErrorLogView e) -> e.getStep().precedence())
                        .thenComparing(x -> x.getRecordId().getId()))
                .collect(groupingBy(ErrorLogView::getStep, LinkedHashMap::new,
                        groupingBy(ErrorLogView::getStatus,
                                groupingBy(ErrorLogView::getMessage))));
    }

    private void addStepInfo(List<ProgressByStepDto> stepsInfo,
                             Map<Status, Long> statusMap,
                             Step step,
                             Map<Step, Map<Status, Map<String, List<ErrorLogView>>>> recordErrorsByStep
    ) {
        stepsInfo
                .add(new ProgressByStepDto(step,
                        statusMap.getOrDefault(Status.SUCCESS, 0L),
                        statusMap.getOrDefault(Status.FAIL, 0L),
                        statusMap.getOrDefault(Status.WARN, 0L),
                        addStepErrors(recordErrorsByStep.get(step))));
    }

    private List<ErrorInfoDto> addStepErrors(Map<Status, Map<String, List<ErrorLogView>>> statusMap) {
        if (isNull(statusMap) || statusMap.isEmpty()) {
            return List.of();
        }

        List<ErrorInfoDto> errorInfoDtoList = new LinkedList<>();

        statusMap.forEach((status, errorsMap) ->
                errorsMap.forEach((error, recordList) -> errorInfoDtoList.add(
                        new ErrorInfoDto(error, status,
                                recordList.stream()
                                        .map(ErrorLogView::getRecordId)
                                        .map(DatasetReportServiceImpl::createMessageRecordError)
                                        .sorted(String::compareTo)
                                        .collect(toList())))));

        errorInfoDtoList.sort(Comparator.comparing(x -> x.getRecordIds().get(FIRST)));
        return errorInfoDtoList;
    }

    private TiersZeroInfo prepareTiersInfo(String datasetId) {
        // get list of records with content tier 0
        List<String> listOfRecordsIdsWithContentZero = recordRepository.findTop10ByDatasetIdAndContentTierOrderByEuropeanaIdAsc(datasetId, MediaTier.T0.toString())
                .stream().map(RecordEntity::getEuropeanaId).collect(Collectors.toUnmodifiableList());
        // get list of records with metadata tier 0
        List<String> listOfRecordsIdsWithMetadataZero = recordRepository.findTop10ByDatasetIdAndMetadataTierOrderByEuropeanaIdAsc(datasetId, MetadataTier.T0.toString())
                .stream().map(RecordEntity::getEuropeanaId).collect(Collectors.toUnmodifiableList());

        // encapsulate values into TierStatistics. Cut list of record ids into limit number
        TierStatistics contentTierInfo = listOfRecordsIdsWithContentZero.isEmpty() ? null :
                new TierStatistics(recordRepository.getRecordWithDatasetIdAndContentTierCount(datasetId, MediaTier.T0.toString()),
                        listOfRecordsIdsWithContentZero);

        // encapsulate values into TierStatistics. Cut list of record ids into limit number
        TierStatistics metadataTierInfo = listOfRecordsIdsWithMetadataZero.isEmpty() ? null :
                new TierStatistics(recordRepository.getRecordWithDatasetIdAndMetadataTierCount(datasetId, MetadataTier.T0.toString()),
                        listOfRecordsIdsWithMetadataZero);

        // encapsulate values into TiersZeroInfo
        return contentTierInfo == null && metadataTierInfo == null ? null :
                new TiersZeroInfo(contentTierInfo, metadataTierInfo);
    }

    private HarvestingParametricDto prepareHarvestingParametersData(HarvestingParameterEntity harvestingParameterEntity){
        switch(harvestingParameterEntity.getProtocol()){
            case FILE:
                return new FileHarvestingDto(harvestingParameterEntity.getFileName(), harvestingParameterEntity.getFileType());

            case HTTP:
                return new HttpHarvestingDto(harvestingParameterEntity.getUrl());

            case OAI_PMH:
                return new OAIPmhHarvestingDto(harvestingParameterEntity.getUrl(), harvestingParameterEntity.getSetSpec(),
                        harvestingParameterEntity.getMetadataFormat());

            default:
                throw new  ServiceException(format("Failed to get harvesting parameters for dataset id: [%s]. ",
                        harvestingParameterEntity.getDatasetId().getDatasetId()));
        }
    }
}
