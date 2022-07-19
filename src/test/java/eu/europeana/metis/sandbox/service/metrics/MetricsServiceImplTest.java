package eu.europeana.metis.sandbox.service.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.aggregation.DatasetProblemPatternStatistic;
import eu.europeana.metis.sandbox.common.aggregation.DatasetStatistic;
import eu.europeana.metis.sandbox.common.aggregation.StepStatistic;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.patternanalysis.view.ProblemPatternDescription.ProblemPatternId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link MetricsServiceImpl} class
 */
@ExtendWith(MockitoExtension.class)
class MetricsServiceImplTest {

  @Mock
  private RecordRepository recordRepository;
  @Mock
  private RecordLogRepository recordLogRepository;
  @Mock
  private DatasetProblemPatternRepository problemPatternRepository;
  @Spy
  private MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @InjectMocks
  private MetricsServiceImpl metricsService;

  private void assertRepositoriesAndMeterRegistry() {
    verify(recordRepository, times(2)).getMetricDatasetStatistics();
    verify(recordLogRepository, times(2)).getMetricStepStatistics();
    verify(problemPatternRepository, times(2)).getMetricProblemPatternStatistics();
    assertEquals(43, meterRegistry.getMeters().size());
  }

  @Test
  void processMetrics() throws InterruptedException {
    when(recordRepository.getMetricDatasetStatistics()).thenReturn(List.of(new DatasetStatistic("1", 10L)));
    when(recordLogRepository.getMetricStepStatistics()).thenReturn(
        List.of(new StepStatistic(Step.HARVEST_ZIP, Status.SUCCESS, 10L)));
    when(problemPatternRepository.getMetricProblemPatternStatistics()).thenReturn(List.of(
        new DatasetProblemPatternStatistic(ProblemPatternId.P2.name(), 5L)));

    metricsService.processMetrics();

    assertEquals(1L, meterRegistry.get("sandbox.metrics.dataset.count").gauge().value());
    assertEquals(10L, meterRegistry.get("sandbox.metrics.dataset.harvest_zip.success").gauge().value());
    assertEquals(5L, meterRegistry.get("sandbox.metrics.dataset.p2").gauge().value());
    assertRepositoriesAndMeterRegistry();
  }

  @Test
  void processMetrics_whenDatasetNull() {
    when(recordRepository.getMetricDatasetStatistics()).thenReturn(null);
    metricsService.processMetrics();

    assertEquals(0L, metricsService.getTotalRecords());
    assertEquals(0L, meterRegistry.get("sandbox.metrics.dataset.count").gauge().value());
    assertRepositoriesAndMeterRegistry();
  }

  @Test
  void processMetrics_whenStepNull() {
    when(recordLogRepository.getMetricStepStatistics()).thenReturn(null);
    metricsService.processMetrics();

    assertEquals(0L, metricsService.getTotalRecords(Step.HARVEST_ZIP, Status.SUCCESS));
    assertEquals(0L, meterRegistry.get("sandbox.metrics.dataset.harvest_zip.success").gauge().value());
    assertRepositoriesAndMeterRegistry();
  }

  @Test
  void processMetrics_whenProblemPatternNull() {
    when(problemPatternRepository.getMetricProblemPatternStatistics()).thenReturn(null);
    metricsService.processMetrics();

    assertEquals(0L, metricsService.getTotalOccurrences(ProblemPatternId.P2));
    assertEquals(0L, meterRegistry.get("sandbox.metrics.dataset.p2").gauge().value());
    assertRepositoriesAndMeterRegistry();
  }

  @Test
  void proccessMetrics_whenDataAvailable() {
    when(recordRepository.getMetricDatasetStatistics()).thenReturn(List.of(new DatasetStatistic("1", 5L),
        new DatasetStatistic("2", 5L)));
    when(recordLogRepository.getMetricStepStatistics()).thenReturn(
        List.of(new StepStatistic(Step.HARVEST_ZIP, Status.SUCCESS, 10L)));
    when(problemPatternRepository.getMetricProblemPatternStatistics()).thenReturn(List.of(
        new DatasetProblemPatternStatistic(ProblemPatternId.P2.name(), 5L)));

    metricsService.processMetrics();

    assertEquals(10L, metricsService.getTotalRecords(Step.HARVEST_ZIP, Status.SUCCESS));
    assertEquals(2L, metricsService.getDatasetCount());
    assertEquals(5L, metricsService.getTotalOccurrences(ProblemPatternId.P2));
    assertRepositoriesAndMeterRegistry();
  }

  @Test
  void processMetrics_throwDatasetException() {
    when(recordRepository.getMetricDatasetStatistics())
        .thenThrow(new RuntimeException("Error Record"));

    assertThrows(RuntimeException.class, () -> metricsService.processMetrics());
  }

  @Test
  void processMetrics_throwStepException() {
    when(recordLogRepository.getMetricStepStatistics())
        .thenThrow(new RuntimeException("Error RecordLog"));

    assertThrows(RuntimeException.class, () -> metricsService.processMetrics());
  }

  @Test
  void processMetrics_throwPatternException() {
    when(problemPatternRepository.getMetricProblemPatternStatistics())
        .thenThrow(new RuntimeException("Error Problem Pattern"));

    assertThrows(RuntimeException.class, () -> metricsService.processMetrics());
  }
}
