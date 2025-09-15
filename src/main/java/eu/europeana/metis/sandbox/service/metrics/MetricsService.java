package eu.europeana.metis.sandbox.service.metrics;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

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
import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for generating metrics and registering them to the meter registry.
 */
@Slf4j
public class MetricsService {

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
    registerMetrics();
  }

  private void registerMetrics() {
    try {
      registerGauge("count", "Dataset count", BASE_UNIT_DATASET, this::getDatasetCount);
      registerGauge("total_records", "Total of Records", BASE_UNIT_RECORD, this::getTotalRecords);

      for (FullBatchJobType jobType : FullBatchJobType.values()) {
        registerStepMetricGauge(jobType, Status.SUCCESS);
        registerStepMetricGauge(jobType, Status.WARN);
        registerStepMetricGauge(jobType, Status.FAIL);
      }

      for (ProblemPatternId patternId : ProblemPatternId.values()) {
        String patternMetricName = getPatternMetricName(patternId);
        String patternDescription = format("Processed records with problem pattern %s: %s",
            patternId.name(), ProblemPatternDescription.fromName(patternId.name()).getProblemPatternTitle());
        registerGauge(patternMetricName, patternDescription, BASE_UNIT_RECORD, () -> getTotalOccurrences(patternId)
        );
      }
    } catch (RuntimeException ex) {
      log.error("Unable to init metrics", ex);
    }
  }

  /**
   * Refreshes metrics including dataset counts, record totals, step statistics, and problem pattern occurrences for monitoring
   * purposes.
   */
  public void refreshStatistics() {
    refreshDatabaseStatistics();
    log.debug("Refreshed metrics statistics");
  }

  private void refreshDatabaseStatistics() {
    datasetStatistics = executionRecordRepository.getDatasetStatistics();
    problemPatternStatistics = problemPatternRepository.getProblemPatternStatistics();

    successStepCounts = mapStepStatistics(executionRecordRepository.getStepStatistics());
    warningStepCounts = mapStepStatistics(executionRecordWarningRepository.getStepStatistics());
    errorStepCounts = mapStepStatistics(executionRecordErrorRepository.getStepStatistics());
  }

  private Map<FullBatchJobType, Long> mapStepStatistics(List<StepStatisticProjection> projections) {
    return projections.stream()
                      .map(this::toJobTypeOrNull)
                      .filter(Objects::nonNull)
                      .collect(toMap(
                          Map.Entry::getKey,
                          Map.Entry::getValue,
                          (a, b) -> {
                            log.warn("Duplicate step detected, keeping first: {}", a);
                            return a;
                          },
                          () -> new EnumMap<>(FullBatchJobType.class)
                      ));
  }

  private Map.Entry<FullBatchJobType, Long> toJobTypeOrNull(StepStatisticProjection p) {
    try {
      return Map.entry(FullBatchJobType.valueOf(p.getStep()), p.getCount());
    } catch (IllegalArgumentException e) {
      log.warn("Ignoring unknown step '{}'", p.getStep());
      return null;
    }
  }

  private void registerStepMetricGauge(FullBatchJobType jobType, Status status) {
    Supplier<Number> supplier = () -> {
      Map<FullBatchJobType, Long> map = switch (status) {
        case SUCCESS -> successStepCounts;
        case WARN -> warningStepCounts;
        case FAIL -> errorStepCounts;
      };
      return map != null ? map.getOrDefault(jobType, 0L) : 0L;
    };
    registerGauge(getStepMetricName(jobType, status),
        format("%s processed records with status %s", jobType.name(), status.name()),
        BASE_UNIT_RECORD, supplier);
  }

  private void registerGauge(String name, String description, String unit, Supplier<Number> supplier) {
    Gauge.builder(getMetricName(name), supplier)
         .description(description)
         .baseUnit(unit)
         .register(meterRegistry);
  }

  private long getDatasetCount() {
    return ofNullable(datasetStatistics).map(List::size).orElse(0);
  }

  private long getTotalRecords() {
    return Stream.ofNullable(datasetStatistics)
                 .flatMap(List::stream)
                 .mapToLong(DatasetStatisticProjection::getCount)
                 .sum();
  }

  private long getTotalOccurrences(ProblemPatternId patternId) {
    return Stream.ofNullable(problemPatternStatistics)
                 .flatMap(List::stream)
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

