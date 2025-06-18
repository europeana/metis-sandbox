package eu.europeana.metis.sandbox.service.metrics;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository.DatasetStatisticProjection;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository.StepStatisticProjection;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningRepository;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository.DatasetProblemPatternStatisticProjection;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import eu.europeana.patternanalysis.view.ProblemPatternDescription.ProblemPatternId;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for generating metrics and registering them to the meter registry.
 */
public class MetricsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String METRICS_NAMESPACE = "sandbox.metrics.dataset";
  public static final String BASE_UNIT_RECORD = "Record";
  public static final String BASE_UNIT_DATASET = "Dataset";

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordErrorRepository executionRecordErrorRepository;
  private final ExecutionRecordWarningRepository executionRecordWarningRepository;
  private final DatasetProblemPatternRepository problemPatternRepository;
  private final MeterRegistry meterRegistry;

  private List<DatasetStatisticProjection> datasetStatistics;
  private List<DatasetProblemPatternStatisticProjection> problemPatternStatistics;
  private Map<FullBatchJobType, Long> successStepCounts;
  private Map<FullBatchJobType, Long> warningStepCounts;
  private Map<FullBatchJobType, Long> errorStepCounts;

  /**
   * Constructor.
   *
   * @param executionRecordRepository repository for handling execution records
   * @param executionRecordErrorRepository repository for handling execution record exceptions
   * @param executionRecordWarningRepository repository for handling execution record warnings
   * @param problemPatternRepository repository for managing dataset problem patterns
   * @param meterRegistry registry for managing metrics
   */
  public MetricsService(
      ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordErrorRepository executionRecordErrorRepository,
      ExecutionRecordWarningRepository executionRecordWarningRepository,
      DatasetProblemPatternRepository problemPatternRepository,
      MeterRegistry meterRegistry) {
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordErrorRepository = executionRecordErrorRepository;
    this.executionRecordWarningRepository = executionRecordWarningRepository;
    this.problemPatternRepository = problemPatternRepository;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Generates and registers various metrics including dataset counts, record totals, step statistics, and problem pattern
   * occurrences for monitoring purposes.
   *
   * <p>Initializes database statistics and registers global gauge metrics for dataset count and total records.
   * <p>Iterates over all FullBatchJobType values to register step metric gauges for different statuses.
   * <p>Iterates over all ProblemPatternId values to register gauges for tracking occurrences of specific problem patterns.
   */
  public void generateMetrics() {
    try {
      getDatabaseStatistics();
      registerGauge("count", "Dataset count", BASE_UNIT_DATASET, this::getDatasetCount);
      registerGauge("total_records", "Total of Records", BASE_UNIT_RECORD, this::getTotalRecords);

      for (FullBatchJobType jobType : FullBatchJobType.values()) {
        registerStepMetricGauge(jobType, Status.SUCCESS, successStepCounts);
        registerStepMetricGauge(jobType, Status.WARN, warningStepCounts);
        registerStepMetricGauge(jobType, Status.FAIL, errorStepCounts);
      }

      for (ProblemPatternId patternId : ProblemPatternId.values()) {
        registerGauge(
            getPatternMetricName(patternId),
            format("Processed records with problem pattern %s: %s",
                patternId.name(),
                ProblemPatternDescription.fromName(patternId.name()).getProblemPatternTitle()),
            BASE_UNIT_RECORD,
            () -> getTotalOccurrences(patternId)
        );
      }
    } catch (RuntimeException ex) {
      LOGGER.error("Unable to init metrics", ex);
    }
  }

  private void getDatabaseStatistics() {
    datasetStatistics = executionRecordRepository.getDatasetStatistics();
    problemPatternStatistics = problemPatternRepository.getProblemPatternStatistics();

    successStepCounts = mapStepStatistics(executionRecordRepository.getStepStatistics());
    warningStepCounts = mapStepStatistics(executionRecordWarningRepository.getStepStatistics());
    errorStepCounts = mapStepStatistics(executionRecordErrorRepository.getStepStatistics());

    LOGGER.debug("metrics report retrieval");
  }

  private Map<FullBatchJobType, Long> mapStepStatistics(List<StepStatisticProjection> stepStatisticProjections) {
    return stepStatisticProjections.stream()
                                   .collect(Collectors.toMap(
                                       stepStatisticProjection -> FullBatchJobType.valueOf(stepStatisticProjection.getStep()),
                                       StepStatisticProjection::getCount,
                                       (a, b) -> {
                                         throw new IllegalStateException("Duplicate step name detected: " + a);
                                       }
                                   ));
  }

  private void registerStepMetricGauge(FullBatchJobType jobType, Status status, Map<FullBatchJobType, Long> jobTypeLongMap) {
    Supplier<Number> supplier = () -> jobTypeLongMap.getOrDefault(jobType, 0L);
    registerGauge(getStepMetricName(jobType, status),
        format("%s processed records with status %s", jobType.name(), status.name()),
        BASE_UNIT_RECORD,
        supplier);
  }

  private void registerGauge(String name, String description, String unit, Supplier<Number> supplier) {
    Gauge.builder(getMetricName(name), supplier)
         .description(description)
         .baseUnit(unit)
         .register(meterRegistry);
  }

  private long getDatasetCount() {
    return datasetStatistics == null ? 0 : datasetStatistics.size();
  }

  private long getTotalRecords() {
    return datasetStatistics == null ? 0 : datasetStatistics.stream().mapToLong(DatasetStatisticProjection::getCount).sum();
  }

  private long getTotalOccurrences(ProblemPatternId patternId) {
    return problemPatternStatistics == null ?
        0 : problemPatternStatistics.stream()
                                    .filter(p -> patternId.name().equals(p.getPatternId()))
                                    .mapToLong(DatasetProblemPatternStatisticProjection::getTotalOccurrences)
                                    .sum();
  }

  private String getMetricName(String name) {
    return METRICS_NAMESPACE + "." + name;
  }

  private String getStepMetricName(FullBatchJobType fullBatchJobType, Status status) {
    return fullBatchJobType.name().toLowerCase(Locale.US) + "." + status.name().toLowerCase(Locale.US);
  }

  private String getPatternMetricName(ProblemPatternId patternId) {
    return patternId.name().toLowerCase(Locale.US);
  }
}

