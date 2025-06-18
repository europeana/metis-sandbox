package eu.europeana.metis.sandbox.service.metrics;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository.DatasetStatisticProjection;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository.StepStatisticProjection;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository.DatasetProblemPatternStatisticProjection;
import eu.europeana.patternanalysis.view.ProblemPatternDescription.ProblemPatternId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

  @Mock
  private ExecutionRecordRepository executionRecordRepository;
  @Mock
  private ExecutionRecordErrorRepository executionRecordErrorRepository;
  @Mock
  private ExecutionRecordWarningRepository executionRecordWarningRepository;
  @Mock
  private DatasetProblemPatternRepository problemPatternRepository;

  private MeterRegistry meterRegistry;

  private MetricsService metricsService;

  @BeforeEach
  void setup() {
    meterRegistry = new SimpleMeterRegistry();
    metricsService = new MetricsService(
        executionRecordRepository,
        executionRecordErrorRepository,
        executionRecordWarningRepository,
        problemPatternRepository,
        meterRegistry
    );
  }

  @Test
  void getDatabaseStatistics() {
    // Mock dataset statistics
    DatasetStatisticProjection dataset1 = mock(DatasetStatisticProjection.class);
    when(dataset1.getCount()).thenReturn(10L);
    when(executionRecordRepository.getDatasetStatistics()).thenReturn(List.of(dataset1));

    // Mock problem pattern statistics
    DatasetProblemPatternStatisticProjection pattern1 = mock(DatasetProblemPatternStatisticProjection.class);
    when(pattern1.getPatternId()).thenReturn(ProblemPatternId.P2.name());
    when(pattern1.getTotalOccurrences()).thenReturn(3L);
    when(problemPatternRepository.getProblemPatternStatistics()).thenReturn(List.of(pattern1));

    // Mock step statistics
    StepStatisticProjection step1 = mock(StepStatisticProjection.class);
    when(step1.getStep()).thenReturn(FullBatchJobType.HARVEST_FILE.name());
    when(step1.getCount()).thenReturn(5L);
    when(executionRecordRepository.getStepStatistics()).thenReturn(List.of(step1));
    when(executionRecordWarningRepository.getStepStatistics()).thenReturn(List.of());
    when(executionRecordErrorRepository.getStepStatistics()).thenReturn(List.of());

    metricsService.generateMetrics();

    // Assert gauges
    String metricType = FullBatchJobType.HARVEST_FILE.name().toLowerCase(Locale.US);
    assertEquals(1.0, getGaugeValue("sandbox.metrics.dataset.count"));
    assertEquals(10.0, getGaugeValue("sandbox.metrics.dataset.total_records"));
    assertEquals(5.0, getGaugeValue(format("sandbox.metrics.dataset.%s.success", metricType)));
    assertEquals(0.0, getGaugeValue(format("sandbox.metrics.dataset.%s.warn", metricType)));
    assertEquals(0.0, getGaugeValue(format("sandbox.metrics.dataset.%s.fail", metricType)));
    assertEquals(3.0, getGaugeValue("sandbox.metrics.dataset.p2"));

    // Verify repository calls
    verify(executionRecordRepository, times(1)).getDatasetStatistics();
    verify(problemPatternRepository, times(1)).getProblemPatternStatistics();
    verify(executionRecordRepository, times(1)).getStepStatistics();
    verify(executionRecordWarningRepository, times(1)).getStepStatistics();
    verify(executionRecordErrorRepository, times(1)).getStepStatistics();
  }

  @Test
  void getDatabaseStatistics_statisticsNull() {
    metricsService.generateMetrics();
    String metricType = FullBatchJobType.HARVEST_FILE.name().toLowerCase(Locale.US);
    assertEquals(0.0, getGaugeValue("sandbox.metrics.dataset.count"));
    assertEquals(0.0, getGaugeValue("sandbox.metrics.dataset.total_records"));
    assertEquals(0.0, getGaugeValue(format("sandbox.metrics.dataset.%s.success", metricType)));
    assertEquals(0.0, getGaugeValue(format("sandbox.metrics.dataset.%s.warn", metricType)));
    assertEquals(0.0, getGaugeValue(format("sandbox.metrics.dataset.%s.fail", metricType)));
    assertEquals(0.0, getGaugeValue("sandbox.metrics.dataset.p2"));
  }

  private double getGaugeValue(String gaugeName) {
    return meterRegistry.get(gaugeName).gauge().value();
  }
}
