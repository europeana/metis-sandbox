package eu.europeana.metis.sandbox.service.metrics;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
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
  void refreshDatabaseStatistics() {
    DatasetStatisticProjection dataset1 = mock(DatasetStatisticProjection.class);
    DatasetStatisticProjection dataset2 = mock(DatasetStatisticProjection.class);
    when(dataset1.getCount()).thenReturn(5L);
    when(dataset2.getCount()).thenReturn(10L);

    StepStatisticProjection stepProjection = mock(StepStatisticProjection.class);
    when(stepProjection.getStep()).thenReturn(FullBatchJobType.HARVEST_FILE.name());
    when(stepProjection.getCount()).thenReturn(7L);

    DatasetProblemPatternStatisticProjection patternProjection =
        mock(DatasetProblemPatternStatisticProjection.class);
    when(patternProjection.getPatternId()).thenReturn(ProblemPatternId.P1.name());
    when(patternProjection.getTotalOccurrences()).thenReturn(3L);

    when(executionRecordRepository.getDatasetStatistics()).thenReturn(List.of(dataset1, dataset2));
    when(executionRecordRepository.getStepStatistics()).thenReturn(List.of(stepProjection));
    when(problemPatternRepository.getProblemPatternStatistics()).thenReturn(List.of(patternProjection));

    metricsService.refreshStatistics();

    String metricType = FullBatchJobType.HARVEST_FILE.name().toLowerCase(Locale.US);
    assertEquals(2.0, getGaugeValue("sandbox.metrics.dataset.count"));
    assertEquals(15.0, getGaugeValue("sandbox.metrics.dataset.total_records"));
    assertEquals(7.0, getGaugeValue(format("sandbox.metrics.dataset.%s.success", metricType)));
    assertEquals(0.0, getGaugeValue(format("sandbox.metrics.dataset.%s.warn", metricType)));
    assertEquals(0.0, getGaugeValue(format("sandbox.metrics.dataset.%s.fail", metricType)));
    assertEquals(3.0, getGaugeValue("sandbox.metrics.dataset.p1"));

    verify(executionRecordRepository, times(1)).getDatasetStatistics();
    verify(problemPatternRepository, times(1)).getProblemPatternStatistics();
    verify(executionRecordRepository, times(1)).getStepStatistics();
    verify(executionRecordWarningRepository, times(1)).getStepStatistics();
    verify(executionRecordErrorRepository, times(1)).getStepStatistics();
  }

  @Test
  void refreshDatabaseStatistics_statisticsNull() {
    metricsService.refreshStatistics();
    String metricType = FullBatchJobType.HARVEST_FILE.name().toLowerCase(Locale.US);
    assertEquals(0.0, getGaugeValue("sandbox.metrics.dataset.count"));
    assertEquals(0.0, getGaugeValue("sandbox.metrics.dataset.total_records"));
    assertEquals(0.0, getGaugeValue(format("sandbox.metrics.dataset.%s.success", metricType)));
    assertEquals(0.0, getGaugeValue(format("sandbox.metrics.dataset.%s.warn", metricType)));
    assertEquals(0.0, getGaugeValue(format("sandbox.metrics.dataset.%s.fail", metricType)));
    assertEquals(0.0, getGaugeValue("sandbox.metrics.dataset.p1"));
  }

  private double getGaugeValue(String gaugeName) {
    return meterRegistry.get(gaugeName).gauge().value();
  }
}
