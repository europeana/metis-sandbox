package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionLogRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningExceptionRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.service.metrics.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class MetricsServiceConfig {

  private MetricsService metricsService;

  @Bean
  MetricsService metricsService(
      ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository,
      ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository,
      DatasetProblemPatternRepository problemPatternRepository,
      MeterRegistry meterRegistry) {
    metricsService = new MetricsService(executionRecordRepository, executionRecordExceptionLogRepository,
        executionRecordWarningExceptionRepository, problemPatternRepository,
        meterRegistry);
    return metricsService;
  }

  @Scheduled(cron = "${sandbox.metrics.frequency:*/5 * * * * *}")
  void metricsReport() {
    metricsService.getDatabaseMetrics();
  }
}
