package eu.europeana.metis.sandbox.service.metrics;

import eu.europeana.metis.sandbox.common.exception.MetricsException;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto.Status;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.metrics.ProgressDataset;
import eu.europeana.metis.sandbox.entity.metrics.ProgressStep;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.metrics.ProgressDatasetRepository;
import eu.europeana.metis.sandbox.repository.metrics.ProgressStepRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Service;

@Service
@Endpoint(id = "sandbox-metrics")
public class MetricsServiceImpl implements MetricsService {

  @Autowired
  DatasetRepository datasetRepository;
  @Autowired
  DatasetReportService datasetReportService;

  @Autowired
  ProgressDatasetRepository progressDatasetRepository;

  @Autowired
  ProgressStepRepository progressStepRepository;

  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsServiceImpl.class);
  private final Map<String, Lock> metricsLocksMap = new ConcurrentHashMap<>();

  @ReadOperation
  @Override
  public DatasetMetrics datasetMetrics() {
    Map<String, Object> metricsMap = new ConcurrentHashMap<>();
    metricsMap.put("DatasetCount", progressDatasetRepository.count());
    DatasetMetrics datasetMetrics = new DatasetMetrics();
    datasetMetrics.setDatasetMetricsMap(metricsMap);
    return datasetMetrics;
  }

  @Override
  public void processMetrics(String datasetId) {
    ProgressInfoDto report = datasetReportService.getReport(datasetId);
    if (report.getStatus() == Status.COMPLETED) {
      final Lock lock = metricsLocksMap.computeIfAbsent(datasetId, s -> new ReentrantLock());
      try {
        lock.lock();
        LOGGER.debug("process metrics dataset-id:{} lock, Locked", datasetId);
        LOGGER.debug("Report processed:{} total:{}", report.getProcessedRecords(), report.getTotalRecords());
        if (datasetDoesntExists(datasetId)) {
          saveMetricsProgressByStep(datasetId, report);
          Optional<DatasetEntity> datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId));
          LocalDateTime startTimeStamp = datasetEntity.isPresent() ? datasetEntity.get().getCreatedDate() : LocalDateTime.now();
          saveMetricsProgressDataset(datasetId, report, startTimeStamp, LocalDateTime.now());
        }
      } catch (MetricsException e) {
        LOGGER.error("Something went wrong during acquiring metrics", e);
      } finally {
        lock.unlock();
        LOGGER.debug("process metrics: {} lock, Unlocked", datasetId);
      }
    }
  }

  private void saveMetricsProgressByStep(String datasetId, ProgressInfoDto report) {
    report.getProgressByStep().stream().forEach(
        item -> {
          LOGGER.debug("step:{} total:{} success:{} fail:{} warn: {}", item.getStep().value(), item.getTotal(),
              item.getSuccess(), item.getFail(), item.getWarn());
          ProgressStep progressStep = new ProgressStep();
          progressStep.setDatasetId(datasetId);
          progressStep.setStep(item.getStep().value());
          progressStep.setSuccess(item.getSuccess());
          progressStep.setFail(item.getFail());
          progressStep.setWarn(item.getWarn());
          progressStep.setTotal(item.getTotal());
          progressStepRepository.save(progressStep);
        }
    );
  }

  private void saveMetricsProgressDataset(String datasetId,
      ProgressInfoDto report,
      LocalDateTime startTimeStamp,
      LocalDateTime endTimeStamp) {

    ProgressDataset progressDataset = new ProgressDataset();
    progressDataset.setDatasetId(datasetId);
    progressDataset.setProcessedRecords(report.getProcessedRecords());
    progressDataset.setTotalRecords(report.getTotalRecords());
    progressDataset.setStatus(report.getStatus().value());
    progressDataset.setStartTimeStamp(startTimeStamp);
    progressDataset.setEndTimeStamp(endTimeStamp);

    progressDatasetRepository.save(progressDataset);
  }

  private boolean datasetDoesntExists(String datasetId) {
      ProgressDataset progressDataset = progressDatasetRepository.findByDatasetId(datasetId);
      return progressDataset == null;
  }
}
