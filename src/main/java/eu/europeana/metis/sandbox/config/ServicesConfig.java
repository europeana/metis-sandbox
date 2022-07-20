package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.service.metrics.MetricsService;
import eu.europeana.metis.sandbox.service.metrics.MetricsServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class ServicesConfig {

  @Value("${elastic.apm.enabled}")
  private Boolean metricsEnabled;
  private MetricsService metricsService;

  @Bean
  MetricsService metricsService(
      RecordRepository recordRepository,
      RecordLogRepository recordLogRepository,
      DatasetProblemPatternRepository problemPatternRepository,
      MeterRegistry meterRegistry) {
    metricsService = new MetricsServiceImpl(recordRepository, recordLogRepository, problemPatternRepository, meterRegistry);
    return metricsService;
  }

  @Scheduled(cron = "${sandbox.metrics.dataset.frequency:*/5 * * * * *")
  void metricsReport() {
    if (Boolean.TRUE.equals(metricsEnabled)) {
      metricsService.processMetrics();
    }
  }
}
