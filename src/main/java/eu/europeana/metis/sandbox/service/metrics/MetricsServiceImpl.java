package eu.europeana.metis.sandbox.service.metrics;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.metrics.ProgressDataset;
import eu.europeana.metis.sandbox.entity.metrics.ProgressStep;
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
          ProgressStep progressStep = findStepProgress(datasetId, item.getStep());
          if (progressStep == null) {
            progressStep = new ProgressStep();
            progressStep.setDatasetId(datasetId);
            progressStep.setStep(item.getStep().value());
          }
          progressStep.setSuccess(item.getSuccess());
          progressStep.setFail(item.getFail());
          progressStep.setWarn(item.getWarn());
          progressStep.setTotal(item.getTotal());
          progressStepRepository.save(progressStep);
        }
    );
  }

  private void saveMetricsProgressDataset(String datasetId, ProgressInfoDto report) {
    ProgressDataset progressDataset = findProgressDataset(datasetId);
    if (progressDataset == null) {
      Optional<DatasetEntity> datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId));
      LocalDateTime startTimeStamp = datasetEntity.isPresent() ? datasetEntity.get().getCreatedDate() : LocalDateTime.now();
      progressDataset = new ProgressDataset();
      progressDataset.setDatasetId(datasetId);
      progressDataset.setStartTimeStamp(startTimeStamp);
    } else {
      progressDataset = progressDatasetRepository.findByDatasetId(datasetId);
    }
    progressDataset.setProcessedRecords(report.getProcessedRecords());
    progressDataset.setTotalRecords(report.getTotalRecords());
    progressDataset.setStatus(report.getStatus().value());
    progressDataset.setEndTimeStamp(LocalDateTime.now());
    progressDatasetRepository.save(progressDataset);
  }

  private ProgressDataset findProgressDataset(String datasetId) {
    ProgressDataset progressDataset = progressDatasetRepository.findByDatasetId(datasetId);
    return progressDataset;
  }

  private ProgressStep findStepProgress(String datasetId, Step step) {
    ProgressStep progressStep = progressStepRepository.findByDatasetIdAndStep(datasetId, step.value());
    return progressStep;
  }

  private void getProgressDatasetMetrics(Map<String, Object> metricsMap) {
    List<ProgressDataset> progressDatasets = progressDatasetRepository.findAll();

    LongSummaryStatistics totalStatistics = progressDatasets.stream()
                                                            .mapToLong(ProgressDataset::getTotalRecords)
                                                            .summaryStatistics();

    LongSummaryStatistics processedStatistics = progressDatasets.stream()
                                                                .mapToLong(ProgressDataset::getProcessedRecords)
                                                                .summaryStatistics();

    LongSummaryStatistics durationStatistics = progressDatasets.stream()
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
    List<ProgressStep> progressSteps = progressStepRepository.findAll();
    Map<String, Object> stepCategory = new HashMap<>();
    stepMetrics(stepCategory, progressSteps, Step.HARVEST_ZIP);
    stepMetrics(stepCategory, progressSteps, Step.HARVEST_OAI_PMH);
    stepMetrics(stepCategory, progressSteps, Step.VALIDATE_EXTERNAL);
    stepMetrics(stepCategory, progressSteps, Step.TRANSFORM);
    stepMetrics(stepCategory, progressSteps, Step.VALIDATE_INTERNAL);
    stepMetrics(stepCategory, progressSteps, Step.NORMALIZE);
    stepMetrics(stepCategory, progressSteps, Step.ENRICH);
    stepMetrics(stepCategory, progressSteps, Step.MEDIA_PROCESS);
    stepMetrics(stepCategory, progressSteps, Step.PUBLISH);
    metricsMap.put("MetricsByStep", stepCategory);
  }

  private void stepMetrics(Map<String, Object> category, List<ProgressStep> progressSteps, Step step) {
    LongSummaryStatistics totalHarvestZipStatistics = progressSteps.stream()
                                                                   .filter(progressStep -> progressStep.getStep().equals(
                                                                       step.value()))
                                                                   .mapToLong(ProgressStep::getTotal)
                                                                   .summaryStatistics();
    LongSummaryStatistics successHarvestZipStatistics = progressSteps.stream()
                                                                     .filter(progressStep -> progressStep.getStep().equals(
                                                                         step.value()))
                                                                     .mapToLong(ProgressStep::getSuccess)
                                                                     .summaryStatistics();
    LongSummaryStatistics failHarvestZipStatistics = progressSteps.stream()
                                                                  .filter(progressStep -> progressStep.getStep().equals(
                                                                      step.value()))
                                                                  .mapToLong(ProgressStep::getFail)
                                                                  .summaryStatistics();
    LongSummaryStatistics warnHarvestZipStatistics = progressSteps.stream()
                                                                  .filter(progressStep -> progressStep.getStep().equals(
                                                                      step.value()))
                                                                  .mapToLong(ProgressStep::getWarn)
                                                                  .summaryStatistics();

    Map<String, Object> master = new HashMap<>();
    addMetricToOverview(totalHarvestZipStatistics, master, "TotalRecords");
    addMetricToOverview(successHarvestZipStatistics, master, "SuccessRecords");
    addMetricToOverview(failHarvestZipStatistics, master, "FailRecords");
    addMetricToOverview(warnHarvestZipStatistics, master, "WarnRecords");
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
