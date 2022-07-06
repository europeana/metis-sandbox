package eu.europeana.metis.sandbox.service.metrics;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.metrics.ProgressDatasetEntity;
import eu.europeana.metis.sandbox.entity.metrics.ProgressStepEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.metrics.ProgressDatasetRepository;
import eu.europeana.metis.sandbox.repository.metrics.ProgressStepRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Service;

@Service
@Endpoint(id = "sandbox-metrics")
public class MetricsServiceImpl implements MetricsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsServiceImpl.class);

  private final Map<String, Lock> metricsLocksMap = new ConcurrentHashMap<>();

  private final DatasetRepository datasetRepository;

  private final DatasetReportService datasetReportService;

  private final ProgressDatasetRepository progressDatasetRepository;

  private final ProgressStepRepository progressStepRepository;

  public MetricsServiceImpl(DatasetRepository datasetRepository,
      DatasetReportService datasetReportService,
      ProgressDatasetRepository progressDatasetRepository,
      ProgressStepRepository progressStepRepository) {
    this.datasetRepository = datasetRepository;
    this.datasetReportService = datasetReportService;
    this.progressDatasetRepository = progressDatasetRepository;
    this.progressStepRepository = progressStepRepository;
  }

  @ReadOperation
  @Override
  public DatasetMetrics datasetMetrics() {
    Map<String, Object> metricsMap = new ConcurrentHashMap<>();
    DatasetMetrics datasetMetrics = new DatasetMetrics();

    metricsMap.putAll(getMapProgressDatasetMetrics());
    metricsMap.putAll(getMapProgressStepMetrics());
    datasetMetrics.setDatasetMetricsMap(metricsMap);
    return datasetMetrics;
  }

  @DeleteOperation
  public void resetDatasetMetrics() {
    progressDatasetRepository.deleteAll();
    progressStepRepository.deleteAll();
    LOGGER.info("Dataset metrics counters reset completed");
  }

  @Override
  public void processMetrics(String datasetId) {
    ProgressInfoDto report = datasetReportService.getReport(datasetId);
    final Lock lock = metricsLocksMap.computeIfAbsent(datasetId, s -> new ReentrantLock());
    try {
      lock.lock();
      LOGGER.debug("Process metrics dataset-id:{} lock, Locked", datasetId);
      LOGGER.debug("Report processed:{} total:{}", report.getProcessedRecords(), report.getTotalRecords());
      saveMetricsProgressDataset(datasetId, report);
      saveMetricsProgressByStep(datasetId, report);
    } catch (RuntimeException metricsException) {
      LOGGER.error("Something went wrong during processing metrics", metricsException);
    } finally {
      lock.unlock();
      LOGGER.debug("Process metrics: {} lock, Unlocked", datasetId);
    }
  }

  private void saveMetricsProgressByStep(String datasetId, ProgressInfoDto report) {
    report.getProgressByStep().stream().forEach(
        item -> {
          LOGGER.debug("step:{} total:{} success:{} fail:{} warn: {}",
              item.getStep().value(), item.getTotal(),
              item.getSuccess(), item.getFail(), item.getWarn());
          ProgressStepEntity progressStepEntity = progressStepRepository.findByDatasetIdAndStep(datasetId, item.getStep().value());
          if (progressStepEntity == null) {
            progressStepEntity = new ProgressStepEntity();
            progressStepEntity.setDatasetId(datasetId);
            progressStepEntity.setStep(item.getStep().value());
          }
          progressStepEntity.setSuccess(item.getSuccess());
          progressStepEntity.setFail(item.getFail());
          progressStepEntity.setWarn(item.getWarn());
          progressStepEntity.setTotal(item.getTotal());
          progressStepRepository.save(progressStepEntity);
        }
    );
  }

  private void saveMetricsProgressDataset(String datasetId, ProgressInfoDto report) {
    ProgressDatasetEntity progressDatasetEntity = progressDatasetRepository.findByDatasetId(datasetId);
    if (progressDatasetEntity == null) {
      Optional<DatasetEntity> datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId));
      LocalDateTime startTimeStamp = datasetEntity.isPresent() ? datasetEntity.get().getCreatedDate() : LocalDateTime.now();
      progressDatasetEntity = new ProgressDatasetEntity();
      progressDatasetEntity.setDatasetId(datasetId);
      progressDatasetEntity.setStartTimeStamp(startTimeStamp);
    }
    progressDatasetEntity.setProcessedRecords(report.getProcessedRecords());
    progressDatasetEntity.setTotalRecords(report.getTotalRecords());
    progressDatasetEntity.setStatus(report.getStatus().value());
    progressDatasetEntity.setEndTimeStamp(LocalDateTime.now());
    progressDatasetRepository.save(progressDatasetEntity);
  }

  private Map<String, Object> getMapProgressDatasetMetrics() {
    Map<String, Object> progressDatasetMetricsMap = new HashMap<>();
    List<ProgressDatasetEntity> progressDatasetEntities = progressDatasetRepository.findAll();

    LongSummaryStatistics totalStatistics =
        calcDatasetStatistics(progressDatasetEntities, ProgressDatasetEntity::getTotalRecords);

    LongSummaryStatistics processedStatistics =
        calcDatasetStatistics(progressDatasetEntities, ProgressDatasetEntity::getProcessedRecords);

    LongSummaryStatistics durationStatistics =
        calcDatasetStatistics(progressDatasetEntities, item -> Duration.between(item.getStartTimeStamp(),
            item.getEndTimeStamp()).toSeconds());

    Map<String, Object> master = new HashMap<>();
    master.putAll(getStatisticsFromCategory(totalStatistics, "TotalRecords"));
    master.putAll(getStatisticsFromCategory(processedStatistics, "ProcessedRecords"));
    master.putAll(getStatisticsFromCategory(durationStatistics, "Duration"));
    master.put("DatasetCount", progressDatasetRepository.count());
    progressDatasetMetricsMap.put("MetricsByDataset", master);
    return progressDatasetMetricsMap;
  }

  private Map<String, Object> getMapProgressStepMetrics() {
    Map<String, Object> progressStepMetrics = new HashMap<>();
    List<ProgressStepEntity> progressStepEntities = progressStepRepository.findAll();
    Map<String, Object> stepCategory = new HashMap<>();
    stepCategory.putAll(getMetricsFromStep(progressStepEntities, Step.HARVEST_ZIP));
    stepCategory.putAll(getMetricsFromStep(progressStepEntities, Step.HARVEST_OAI_PMH));
    stepCategory.putAll(getMetricsFromStep(progressStepEntities, Step.VALIDATE_EXTERNAL));
    stepCategory.putAll(getMetricsFromStep(progressStepEntities, Step.TRANSFORM));
    stepCategory.putAll(getMetricsFromStep(progressStepEntities, Step.VALIDATE_INTERNAL));
    stepCategory.putAll(getMetricsFromStep(progressStepEntities, Step.NORMALIZE));
    stepCategory.putAll(getMetricsFromStep(progressStepEntities, Step.ENRICH));
    stepCategory.putAll(getMetricsFromStep(progressStepEntities, Step.MEDIA_PROCESS));
    stepCategory.putAll(getMetricsFromStep(progressStepEntities, Step.PUBLISH));
    progressStepMetrics.put("MetricsByStep", stepCategory);
    return progressStepMetrics;
  }

  private Map<String, Object> getMetricsFromStep(List<ProgressStepEntity> progressStepEntities, Step step) {
    Map<String, Object> category = new HashMap<>();
    LongSummaryStatistics totalStepStatistics =
        calcStepStatistics(progressStepEntities, filterStep(step), ProgressStepEntity::getTotal);

    LongSummaryStatistics successStepStatistics =
        calcStepStatistics(progressStepEntities, filterStep(step), ProgressStepEntity::getSuccess);

    LongSummaryStatistics failStepStatistics =
        calcStepStatistics(progressStepEntities, filterStep(step), ProgressStepEntity::getFail);

    LongSummaryStatistics warnStepStatistics =
        calcStepStatistics(progressStepEntities, filterStep(step), ProgressStepEntity::getWarn);

    Map<String, Object> master = new HashMap<>();
    master.putAll(getStatisticsFromCategory(totalStepStatistics, "TotalRecords"));
    master.putAll(getStatisticsFromCategory(successStepStatistics, "SuccessRecords"));
    master.putAll(getStatisticsFromCategory(failStepStatistics, "FailRecords"));
    master.putAll(getStatisticsFromCategory(warnStepStatistics, "WarnRecords"));
    category.put(step.value(), master);
    return category;
  }

  private LongSummaryStatistics calcStepStatistics(List<ProgressStepEntity> stepEntities,
      Predicate<ProgressStepEntity> predicate, ToLongFunction<ProgressStepEntity> getNumber) {
    return stepEntities.stream()
                       .filter(predicate)
                       .mapToLong(getNumber)
                       .summaryStatistics();
  }

  private LongSummaryStatistics calcDatasetStatistics(List<ProgressDatasetEntity> datasetEntities,
      ToLongFunction<ProgressDatasetEntity> getNumber) {
    return datasetEntities.stream()
                          .mapToLong(getNumber)
                          .summaryStatistics();
  }

  private Predicate<ProgressStepEntity> filterStep(Step step) {
    return stepEntity -> stepEntity.getStep().equals(step.value());
  }

  private Map<String, Object> getStatisticsFromCategory(LongSummaryStatistics summaryStatistics, String category) {
    Map<String, Object> master = new HashMap<>();
    Map<String, Object> detail = new HashMap<>();
    detail.put("Average", summaryStatistics.getAverage());
    detail.put("Min", summaryStatistics.getMin());
    detail.put("Max", summaryStatistics.getMax());
    detail.put("Sum", summaryStatistics.getSum());
    master.put(category, detail);
    return master;
  }
}
