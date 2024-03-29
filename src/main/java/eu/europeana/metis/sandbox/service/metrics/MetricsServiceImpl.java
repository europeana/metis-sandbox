package eu.europeana.metis.sandbox.service.metrics;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.aggregation.DatasetProblemPatternStatistic;
import eu.europeana.metis.sandbox.common.aggregation.DatasetStatistic;
import eu.europeana.metis.sandbox.common.aggregation.QueueStatistic;
import eu.europeana.metis.sandbox.common.aggregation.StepStatistic;
import eu.europeana.metis.sandbox.config.amqp.AmqpConfiguration;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import eu.europeana.patternanalysis.view.ProblemPatternDescription.ProblemPatternId;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;

/**
 * Metrics Service implementation class
 */

public class MetricsServiceImpl implements MetricsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsServiceImpl.class);
  private static final String METRICS_NAMESPACE = "sandbox.metrics.dataset";
  public static final String BASE_UNIT_RECORD = "Record";
  public static final String BASE_UNIT_DATASET = "Dataset";
  public static final String BASE_UNIT_MESSAGE = "Message";
  private final RecordRepository recordRepository;
  private final RecordLogRepository recordLogRepository;
  private final DatasetProblemPatternRepository problemPatternRepository;
  private final MeterRegistry meterRegistry;
  private List<DatasetStatistic> datasetStatistics;
  private List<StepStatistic> stepStatistics;
  private List<DatasetProblemPatternStatistic> problemPatternStatistics;
  private List<QueueStatistic> queueStatistics;
  private final AmqpConfiguration amqpConfiguration;
  private final AmqpAdmin amqpAdmin;
  public MetricsServiceImpl(
      RecordRepository recordRepository,
      RecordLogRepository recordLogRepository,
      DatasetProblemPatternRepository problemPatternRepository,
      MeterRegistry meterRegistry,
      AmqpConfiguration amqpConfiguration) {
    this.recordRepository = recordRepository;
    this.recordLogRepository = recordLogRepository;
    this.problemPatternRepository = problemPatternRepository;
    this.meterRegistry = meterRegistry;
    this.amqpConfiguration = amqpConfiguration;
    this.amqpAdmin = amqpConfiguration.getAmqpAdmin();
    initMetrics();
  }

  @Override
  public void processMetrics() {
    datasetStatistics = recordRepository.getMetricDatasetStatistics();
    stepStatistics = recordLogRepository.getMetricStepStatistics();
    problemPatternStatistics = problemPatternRepository.getMetricProblemPatternStatistics();
    queueStatistics = refreshQueueStatisticsList();
    LOGGER.debug("metrics report retrieval");
  }

  private Long getDatasetCount() {
    return datasetStatistics == null || datasetStatistics.isEmpty() ? 0L : datasetStatistics.size();
  }

  private Long getTotalRecords() {
    return datasetStatistics == null || datasetStatistics.isEmpty() ? 0L
        : datasetStatistics.stream().mapToLong(DatasetStatistic::getCount).sum();
  }

  private Long getTotalOccurrences(ProblemPatternId problemPatternId) {
    return problemPatternStatistics == null || problemPatternStatistics.isEmpty() ? 0L
        : problemPatternStatistics
            .stream()
            .filter(pattern -> pattern.getPatternId().equals(problemPatternId.name()))
            .mapToLong(DatasetProblemPatternStatistic::getCount).sum();
  }

  private Long getTotalRecords(Step step, Status status) {
    return stepStatistics == null || stepStatistics.isEmpty() ? 0L
        : stepStatistics.stream()
                        .filter(stepStatistic -> stepStatistic.getStep() == step &&
                            stepStatistic.getStatus() == status)
                        .mapToLong(StepStatistic::getCount).sum();
  }

  private Long getTotalMessagesFromQueue(String queueName){
    return queueStatistics == null || queueStatistics.isEmpty() ? 0L :
            queueStatistics.stream().filter(queueStatistic -> queueStatistic.getQueueName().equals(queueName))
                           .toList().getFirst().getCountMessages();
  }

  private List<QueueStatistic> refreshQueueStatisticsList(){
    List<QueueStatistic> newList = new ArrayList<>();
    for (String queue : amqpConfiguration.getAllQueuesNames()){
      QueueInformation queueInformation = amqpAdmin.getQueueInfo(queue);
      if(queueInformation == null){
        LOGGER.error("No such queue {} exists", queue);
      } else {
        newList.add(new QueueStatistic(queue, (long) queueInformation.getMessageCount()));
      }
    }
    return newList;
  }

  private void initMetrics() {
    try {

      processMetrics();
      buildGauge(getMetricName("count"), "Dataset count", BASE_UNIT_DATASET, this::getDatasetCount);
      buildGauge(getMetricName("total_records"), "Total of Records", BASE_UNIT_RECORD, this::getTotalRecords);
      for (Step step : Step.values()) {
        for (Status status : Status.values()) {
          buildGauge(getMetricName(getStepMetricName(step, status)),
              step.name() + " processed records with status " + status.name(),
                  BASE_UNIT_RECORD,
              () -> getTotalRecords(step, status));
        }
      }
      for (ProblemPatternId patternId : ProblemPatternId.values()) {
        buildGauge(getMetricName(getPatternMetricName(patternId)),
            "processed records with problem pattern " + patternId.name() + ":"
                + ProblemPatternDescription.fromName(patternId.name()).getProblemPatternTitle(),
                BASE_UNIT_RECORD,
            () -> getTotalOccurrences(patternId));
      }

      for (String queue : amqpConfiguration.getAllQueuesNames()){
          buildGauge(getQueueMetricsName(queue),
                  "messages in queue " + queue,
                  BASE_UNIT_MESSAGE,
                  () -> getTotalMessagesFromQueue(queue));

      }
    } catch (RuntimeException ex) {
      LOGGER.error("Unable to init metrics", ex);
    }
  }

  private void buildGauge(String metricName, String description, String units, Supplier<Number> gaugeFunction) {
    Gauge.builder(metricName, gaugeFunction)
         .description(description)
         .baseUnit(units)
         .register(meterRegistry);
  }

  private String getMetricName(String name) {
    return METRICS_NAMESPACE + "." + name;
  }

  private String getStepMetricName(Step step, Status status) {
    return step.name().toLowerCase(Locale.US) + "." + status.name().toLowerCase(Locale.US);
  }

  private String getPatternMetricName(ProblemPatternId patternId) {
    return patternId.name().toLowerCase(Locale.US);
  }

  private String getQueueMetricsName(String queueName){
    return queueName.toLowerCase(Locale.US).replace("record", "metrics.queue");
  }
}
