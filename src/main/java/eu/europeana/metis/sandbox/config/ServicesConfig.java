package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.sandbox.config.amqp.AmqpConfiguration;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.service.metrics.MetricsService;
import eu.europeana.metis.sandbox.service.metrics.MetricsServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class ServicesConfig {

  private MetricsService metricsService;

  @Bean
  MetricsService metricsService(
      RecordRepository recordRepository,
      RecordLogRepository recordLogRepository,
      DatasetProblemPatternRepository problemPatternRepository,
      MeterRegistry meterRegistry,
      AmqpConfiguration amqpConfiguration) {
    metricsService = new MetricsServiceImpl(recordRepository, recordLogRepository, problemPatternRepository,
            meterRegistry, amqpConfiguration, amqpConfiguration.getAmqpAdmin());
    return metricsService;
  }

  @Scheduled(cron = "${sandbox.metrics.frequency:*/5 * * * * *}")
  void metricsReport() {
    metricsService.processMetrics();
  }
}
