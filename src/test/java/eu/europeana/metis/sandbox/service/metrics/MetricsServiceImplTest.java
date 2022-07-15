package eu.europeana.metis.sandbox.service.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.DatasetStatistic;
import eu.europeana.metis.sandbox.entity.StepStatistic;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
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
  @Spy
  private MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @InjectMocks
  private MetricsServiceImpl metricsService;

  @Test
  void processMetrics() {
    when(recordRepository.getMetricDatasetStatistics()).thenReturn(List.of(new DatasetStatistic("1",10L)));
    when(recordLogRepository.getMetricStepStatistics()).thenReturn(List.of(new StepStatistic(Step.HARVEST_ZIP, Status.SUCCESS,10L)));
    metricsService.processMetrics();

    assertRepositoriesAndMeterRegistry();
  }

  @Test
  void processMetrics_whenDatasetNull() {
    when(recordRepository.getMetricDatasetStatistics()).thenReturn(null);
    metricsService.processMetrics();

    assertEquals(0L, metricsService.getTotalRecords());

    assertRepositoriesAndMeterRegistry();
  }

  @Test
  void processMetrics_whenStepNull() {
    when(recordLogRepository.getMetricStepStatistics()).thenReturn(null);
    metricsService.processMetrics();

    assertEquals(0L, metricsService.getTotalRecords(Step.HARVEST_ZIP, Status.SUCCESS));

    assertRepositoriesAndMeterRegistry();
  }

  private void assertRepositoriesAndMeterRegistry() {
    verify(recordRepository, times(2)).getMetricDatasetStatistics();
    verify(recordLogRepository, times(2)).getMetricStepStatistics();
    assertEquals(32, meterRegistry.getMeters().size());
  }

  @Test
  void proccessMetrics_whenDataAvailable() {
    when(recordRepository.getMetricDatasetStatistics()).thenReturn(List.of(new DatasetStatistic("1",5L),
        new DatasetStatistic("2",5L)));
    when(recordLogRepository.getMetricStepStatistics()).thenReturn(List.of(new StepStatistic(Step.HARVEST_ZIP, Status.SUCCESS,10L)));
    metricsService.processMetrics();

    assertEquals(10L, metricsService.getTotalRecords(Step.HARVEST_ZIP, Status.SUCCESS));
    assertEquals(2L, metricsService.getDatasetCount());
    assertRepositoriesAndMeterRegistry();
  }

}
