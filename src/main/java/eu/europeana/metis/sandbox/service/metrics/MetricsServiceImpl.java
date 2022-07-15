package eu.europeana.metis.sandbox.service.metrics;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.DatasetStatistic;
import eu.europeana.metis.sandbox.entity.StepStatistic;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternStatistic;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import eu.europeana.patternanalysis.view.ProblemPatternDescription.*;

/**
 * Metrics Service implementation class
 */
@Service
public class MetricsServiceImpl implements MetricsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsServiceImpl.class);
  private static final String METRICS_NAMESPACE = "sandbox.metrics.dataset";
  private final RecordRepository recordRepository;
  private final RecordLogRepository recordLogRepository;
  private final DatasetProblemPatternRepository problemPatternRepository;
  private final MeterRegistry meterRegistry;
  private List<DatasetStatistic> datasetStatistics;
  private List<StepStatistic> stepStatistics;
  private List<DatasetProblemPatternStatistic> problemPatternStatistics;

  public MetricsServiceImpl(
      RecordRepository recordRepository,
      RecordLogRepository recordLogRepository,
      DatasetProblemPatternRepository problemPatternRepository,
      MeterRegistry meterRegistry) {
    this.recordRepository = recordRepository;
    this.recordLogRepository = recordLogRepository;
    this.problemPatternRepository = problemPatternRepository;
    this.meterRegistry = meterRegistry;
    initMetrics();
  }

  @Scheduled(cron = "${" + METRICS_NAMESPACE + ".frequency:*/5 * * * * *")
  @Override
  public void processMetrics() {
    datasetStatistics = recordRepository.getMetricDatasetStatistics();
    stepStatistics = recordLogRepository.getMetricStepStatistics();
    problemPatternStatistics = problemPatternRepository.getMetricProblemPatternStatistics();
    LOGGER.debug("metrics report retrieval");
  }

  Long getDatasetCount() {
    if (datasetStatistics == null || datasetStatistics.isEmpty()) {
      return 0L;
    } else {
      return (long) datasetStatistics.size();
    }
  }

  Long getTotalRecords() {
    if (datasetStatistics == null || datasetStatistics.isEmpty()) {
      return 0L;
    } else {
      return datasetStatistics.stream().mapToLong(DatasetStatistic::getCount).sum();
    }
  }

  Long getTotalOcurrences(ProblemPatternId problemPatternId) {
    if (problemPatternStatistics == null || problemPatternStatistics.isEmpty()) {
      return 0L;
    } else {
      return problemPatternStatistics
          .stream()
          .filter(pattern -> pattern.getPatternId().equals(problemPatternId.name()))
          .mapToLong(DatasetProblemPatternStatistic::getCount).sum();
    }
  }

  Long getTotalRecords(Step step, Status status) {
    if (stepStatistics == null || stepStatistics.isEmpty()) {
      return 0L;
    } else {
      return stepStatistics.stream()
                           .filter(stepStatistic -> stepStatistic.getStep().equals(step) &&
                               stepStatistic.getStatus().equals(status))
                           .mapToLong(StepStatistic::getCount).sum();
    }
  }

  void initMetrics() {
    try {
      processMetrics();
      buildGauge("count", "Dataset count", "Dataset", this::getDatasetCount);
      buildGauge("totalrecords", "Total of Records", "record", this::getTotalRecords);
      for (Status status : Status.values()) {
        buildStepGauge(Step.HARVEST_ZIP, status);
        buildStepGauge(Step.HARVEST_OAI_PMH, status);
        buildStepGauge(Step.TRANSFORM_TO_EDM_EXTERNAL, status);
        buildStepGauge(Step.VALIDATE_EXTERNAL, status);
        buildStepGauge(Step.TRANSFORM, status);
        buildStepGauge(Step.VALIDATE_INTERNAL, status);
        buildStepGauge(Step.NORMALIZE, status);
        buildStepGauge(Step.ENRICH, status);
        buildStepGauge(Step.MEDIA_PROCESS, status);
        buildStepGauge(Step.PUBLISH, status);
      }
      for (ProblemPatternId patternId : ProblemPatternId.values()) {
        buildPatternGauge(patternId);
      }
    } catch (RuntimeException ex) {
      LOGGER.error("Unable to init metrics", ex);
    }
  }

  private void buildGauge(String metricName, String description, String units, Supplier<Number> f) {
    Gauge.builder(getMetricName(metricName), f)
         .description(description)
         .baseUnit(units)
         .register(meterRegistry);
  }

  private void buildStepGauge(Step step, Status status) {
    Gauge.builder(getMetricName(step, status), () -> getTotalRecords(step, status))
         .description(step.name() + " processed records with status " + status.name())
         .baseUnit("record")
         .register(meterRegistry);
  }

  private void buildPatternGauge(ProblemPatternId patternId) {
    Gauge.builder(getMetricName(patternId), () -> getTotalOcurrences(patternId))
         .description("processed records with problem pattern " + patternId.name() + ":"
             + ProblemPatternDescription.fromName(patternId.name()).getProblemPatternTitle())
         .baseUnit("record")
         .register(meterRegistry);
  }

  public String getMetricName(String name) {
    return METRICS_NAMESPACE + "." + name;
  }

  public String getMetricName(Step step, Status status) {
    return METRICS_NAMESPACE + "." + step.name().toLowerCase() + "." + status.name().toLowerCase();
  }
  public String getMetricName(ProblemPatternId patternId) {
    return METRICS_NAMESPACE + "." + patternId.name().toLowerCase();
  }
}
