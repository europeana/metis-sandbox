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
    getProgressDatasetMetrics(metricsMap);
    getProgressStepMetrics(metricsMap);
    DatasetMetrics datasetMetrics = new DatasetMetrics();
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
          ProgressStepEntity progressStepEntity = findProgressStep(datasetId, item.getStep());
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
    ProgressDatasetEntity progressDatasetEntity = findProgressDataset(datasetId);
    if (progressDatasetEntity == null) {
      Optional<DatasetEntity> datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId));
      LocalDateTime startTimeStamp = datasetEntity.isPresent() ? datasetEntity.get().getCreatedDate() : LocalDateTime.now();
      progressDatasetEntity = new ProgressDatasetEntity();
      progressDatasetEntity.setDatasetId(datasetId);
      progressDatasetEntity.setStartTimeStamp(startTimeStamp);
    } else {
      progressDatasetEntity = progressDatasetRepository.findByDatasetId(datasetId);
    }
    progressDatasetEntity.setProcessedRecords(report.getProcessedRecords());
    progressDatasetEntity.setTotalRecords(report.getTotalRecords());
    progressDatasetEntity.setStatus(report.getStatus().value());
    progressDatasetEntity.setEndTimeStamp(LocalDateTime.now());
    progressDatasetRepository.save(progressDatasetEntity);
  }

  private ProgressDatasetEntity findProgressDataset(String datasetId) {
    return progressDatasetRepository.findByDatasetId(datasetId);
  }

  private ProgressStepEntity findProgressStep(String datasetId, Step step) {
    return progressStepRepository.findByDatasetIdAndStep(datasetId, step.value());
  }

  private void getProgressDatasetMetrics(Map<String, Object> metricsMap) {
    List<ProgressDatasetEntity> progressDatasetEntities = progressDatasetRepository.findAll();

    LongSummaryStatistics totalStatistics =
        progressDatasetEntities.stream()
                               .mapToLong(ProgressDatasetEntity::getTotalRecords)
                               .summaryStatistics();

    LongSummaryStatistics processedStatistics =
        progressDatasetEntities.stream()
                               .mapToLong(ProgressDatasetEntity::getProcessedRecords)
                               .summaryStatistics();

    LongSummaryStatistics durationStatistics =
        progressDatasetEntities.stream()
                               .mapToLong(
                                   item -> Duration.between(item.getStartTimeStamp(),
                                       item.getEndTimeStamp()).toSeconds())
                               .summaryStatistics();
    Map<String, Object> master = new HashMap<>();
    addMetricToOverview(totalStatistics, master, "TotalRecords");
    addMetricToOverview(processedStatistics, master, "ProcessedRecords");
    addMetricToOverview(durationStatistics, master, "Duration");
    master.put("DatasetCount", progressDatasetRepository.count());
    metricsMap.put("MetricsByDataset", master);
  }

  private void getProgressStepMetrics(Map<String, Object> metricsMap) {
    List<ProgressStepEntity> progressStepEntities = progressStepRepository.findAll();
    Map<String, Object> stepCategory = new HashMap<>();
    stepMetrics(stepCategory, progressStepEntities, Step.HARVEST_ZIP);
    stepMetrics(stepCategory, progressStepEntities, Step.HARVEST_OAI_PMH);
    stepMetrics(stepCategory, progressStepEntities, Step.VALIDATE_EXTERNAL);
    stepMetrics(stepCategory, progressStepEntities, Step.TRANSFORM);
    stepMetrics(stepCategory, progressStepEntities, Step.VALIDATE_INTERNAL);
    stepMetrics(stepCategory, progressStepEntities, Step.NORMALIZE);
    stepMetrics(stepCategory, progressStepEntities, Step.ENRICH);
    stepMetrics(stepCategory, progressStepEntities, Step.MEDIA_PROCESS);
    stepMetrics(stepCategory, progressStepEntities, Step.PUBLISH);
    metricsMap.put("MetricsByStep", stepCategory);
  }

  private void stepMetrics(Map<String, Object> category, List<ProgressStepEntity> progressStepEntities, Step step) {
    LongSummaryStatistics totalStepStatistics =
        progressStepEntities.stream()
                            .filter(progressStepEntity ->
                                progressStepEntity.getStep().equals(step.value()))
                            .mapToLong(ProgressStepEntity::getTotal)
                            .summaryStatistics();

    LongSummaryStatistics successStepStatistics =
        progressStepEntities.stream()
                            .filter(progressStepEntity ->
                                progressStepEntity.getStep().equals(step.value()))
                            .mapToLong(ProgressStepEntity::getSuccess)
                            .summaryStatistics();

    LongSummaryStatistics failStepStatistics =
        progressStepEntities.stream()
                            .filter(progressStepEntity ->
                                progressStepEntity.getStep().equals(step.value()))
                            .mapToLong(ProgressStepEntity::getFail)
                            .summaryStatistics();

    LongSummaryStatistics warnStepStatistics =
        progressStepEntities.stream()
                            .filter(progressStepEntity ->
                                progressStepEntity.getStep().equals(step.value()))
                            .mapToLong(ProgressStepEntity::getWarn)
                            .summaryStatistics();

    Map<String, Object> master = new HashMap<>();
    addMetricToOverview(totalStepStatistics, master, "TotalRecords");
    addMetricToOverview(successStepStatistics, master, "SuccessRecords");
    addMetricToOverview(failStepStatistics, master, "FailRecords");
    addMetricToOverview(warnStepStatistics, master, "WarnRecords");
    category.put(step.value(), master);
  }

  private void addMetricToOverview(LongSummaryStatistics summaryStatistics,
      Map<String, Object> master,
      String category) {
    Map<String, Object> detail = new HashMap<>();
    detail.put("Average", summaryStatistics.getAverage());
    detail.put("Min", summaryStatistics.getMin());
    detail.put("Max", summaryStatistics.getMax());
    detail.put("Sum", summaryStatistics.getSum());
    master.put(category, detail);
  }
}
