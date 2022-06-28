package eu.europeana.metis.sandbox.service.metrics;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto.Status;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import java.util.Map;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsServiceImpl.class);
  private final Map<String, Lock> metricsLocksMap = new ConcurrentHashMap<>();

  @ReadOperation
  @Override
  public DatasetMetrics datasetMetrics() {
    Map<String, Object> metricsMap = new ConcurrentHashMap<>();
    metricsMap.put("DatasetCount", datasetRepository.count());
    DatasetMetrics datasetMetrics = new DatasetMetrics();
    datasetMetrics.setDatasetMetricsMap(metricsMap);
    return datasetMetrics;
  }

  @Override
  public void processMetrics(String datasetId, Step step) {
    ProgressInfoDto report = datasetReportService.getReport(datasetId);
    if (report.getStatus() == Status.COMPLETED) {
      final Lock lock = metricsLocksMap.computeIfAbsent(datasetId, s -> new ReentrantLock());
      try {
        lock.lock();
        LOGGER.debug("process metrics dataset-id:{} lock, Locked", datasetId);
        LOGGER.info("Report processed:{} total:{}", report.getProcessedRecords(), report.getTotalRecords());
        report.getProgressByStep().stream().forEach(
            item -> {
              LOGGER.info("step:{} total:{} success:{} fail:{} warn: {}", item.getStep().value(), item.getTotal(),
                  item.getSuccess(), item.getFail(), item.getWarn());
            }
        );

      } catch (RuntimeException e) {
        LOGGER.error("Something went wrong during acquiring metrics", e);
      } finally {
        lock.unlock();
        LOGGER.debug("process metrics: {} lock, Unlocked", datasetId);
      }
    }
  }
}
