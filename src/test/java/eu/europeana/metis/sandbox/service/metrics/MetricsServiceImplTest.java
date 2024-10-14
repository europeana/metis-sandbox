package eu.europeana.metis.sandbox.service.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.aggregation.DatasetProblemPatternStatistic;
import eu.europeana.metis.sandbox.common.aggregation.DatasetStatistic;
import eu.europeana.metis.sandbox.common.aggregation.StepStatistic;
import eu.europeana.metis.sandbox.config.amqp.AmqpConfiguration;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.patternanalysis.view.ProblemPatternDescription.ProblemPatternId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.*;

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
  private AmqpConfiguration amqpConfiguration = new AmqpConfigurationMockClass();
  @Spy
  private MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @InjectMocks
  private MetricsServiceImpl metricsService;

  private void assertRepositoriesAndMeterRegistry() {
    verify(recordRepository, times(2)).getMetricDatasetStatistics();
    verify(recordLogRepository, times(2)).getMetricStepStatistics();
    verify(problemPatternRepository, times(2)).getMetricProblemPatternStatistics();
    assertEquals(76, meterRegistry.getMeters().size());
  }

  @Test
  void processMetrics() {
    when(recordRepository.getMetricDatasetStatistics()).thenReturn(List.of(new DatasetStatistic("1", 10L)));
    when(recordLogRepository.getMetricStepStatistics()).thenReturn(
        List.of(new StepStatistic(Step.HARVEST_FILE, Status.SUCCESS, 10L)));
    when(problemPatternRepository.getMetricProblemPatternStatistics()).thenReturn(List.of(
        new DatasetProblemPatternStatistic(ProblemPatternId.P2.name(), 5L)));
    when(amqpConfiguration.getAllQueuesNames()).thenReturn(List.of("sandbox.record.created"));

    metricsService.processMetrics();

    assertEquals(1L, meterRegistry.get("sandbox.metrics.dataset.count").gauge().value());
    assertEquals(10L, meterRegistry.get("sandbox.metrics.dataset.harvest_file.success").gauge().value());
    assertEquals(5L, meterRegistry.get("sandbox.metrics.dataset.p2").gauge().value());
    assertEquals(10L, meterRegistry.get("sandbox.metrics.queue.created").gauge().value());
    assertRepositoriesAndMeterRegistry();
  }

  @Test
  void processMetrics_whenDatasetNull() {
    when(recordRepository.getMetricDatasetStatistics()).thenReturn(null);
    metricsService.processMetrics();

    assertEquals(0L, meterRegistry.get("sandbox.metrics.dataset.count").gauge().value());
    assertRepositoriesAndMeterRegistry();
  }

  @Test
  void processMetrics_whenStepNull() {
    when(recordLogRepository.getMetricStepStatistics()).thenReturn(null);
    metricsService.processMetrics();

    assertEquals(0L, meterRegistry.get("sandbox.metrics.dataset.harvest_file.success").gauge().value());
    assertRepositoriesAndMeterRegistry();
  }

  @Test
  void processMetrics_whenProblemPatternNull() {
    when(problemPatternRepository.getMetricProblemPatternStatistics()).thenReturn(null);
    metricsService.processMetrics();

    assertEquals(0L, meterRegistry.get("sandbox.metrics.dataset.p2").gauge().value());
    assertRepositoriesAndMeterRegistry();
  }

  @Test
  void processMetrics_whenDataAvailable() {
    when(recordRepository.getMetricDatasetStatistics()).thenReturn(List.of(new DatasetStatistic("1", 5L),
        new DatasetStatistic("2", 5L)));
    when(recordLogRepository.getMetricStepStatistics()).thenReturn(
        List.of(new StepStatistic(Step.HARVEST_FILE, Status.SUCCESS, 10L)));
    when(problemPatternRepository.getMetricProblemPatternStatistics()).thenReturn(List.of(
        new DatasetProblemPatternStatistic(ProblemPatternId.P2.name(), 5L)));
    when(amqpConfiguration.getAllQueuesNames()).thenReturn(List.of("sandbox.record.created"));

    metricsService.processMetrics();

    assertEquals(2L, meterRegistry.get("sandbox.metrics.dataset.count").gauge().value());
    assertEquals(10L, meterRegistry.get("sandbox.metrics.dataset.harvest_file.success").gauge().value());
    assertEquals(5L, meterRegistry.get("sandbox.metrics.dataset.p2").gauge().value());
    assertEquals(10L, meterRegistry.get("sandbox.metrics.queue.created").gauge().value());
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

  private static class AmqpConfigurationMockClass extends AmqpConfiguration{

    public AmqpConfigurationMockClass(){
      super(null, null);
    }

    @Override
    public List<String> getAllQueuesNames(){
      return List.of("sandbox.record.created", "sandbox.record.created.dlq", "sandbox.record.transformation.edm.external",
              "sandbox.record.transformation.edm.external.dlq", "sandbox.record.validated.external",
              "sandbox.record.validated.external.dlq", "sandbox.record.validated.internal", "sandbox.record.validated.internal.dlq",
              "sandbox.record.transformed", "sandbox.record.transformed.dlq", "sandbox.record.normalized", "sandbox.record.normalized.dlq",
              "sandbox.record.enriched", "sandbox.record.enriched.dlq", "sandbox.record.media.processed", "sandbox.record.media.processed.dlq",
              "sandbox.record.published", "sandbox.record.published.dlq");
    }

    @Override
    public AmqpAdmin getAmqpAdmin(){
      return new AmqpAdminMockClass();
    }

    private static class AmqpAdminMockClass implements AmqpAdmin{

      private final Map<String, QueueInformation> queuesProperties =
              Map.of("sandbox.record.created", new QueueInformation("sandbox.record.created", 10, 1));

      @Override
      public void declareExchange(Exchange exchange) {

      }

      @Override
      public boolean deleteExchange(String exchangeName) {
        return false;
      }

      @Override
      public Queue declareQueue() {
        return null;
      }

      @Override
      public String declareQueue(Queue queue) {
        return null;
      }

      @Override
      public boolean deleteQueue(String queueName) {
        return false;
      }

      @Override
      public void deleteQueue(String queueName, boolean unused, boolean empty) {

      }

      @Override
      public void purgeQueue(String queueName, boolean noWait) {

      }

      @Override
      public int purgeQueue(String queueName) {
        return 0;
      }

      @Override
      public void declareBinding(Binding binding) {

      }

      @Override
      public void removeBinding(Binding binding) {

      }

      @Override
      public Properties getQueueProperties(String queueName) {
        return null;
      }

      @Override
      public QueueInformation getQueueInfo(String queueName) {
        return queuesProperties.get(queueName);
      }
    }
  }
}
