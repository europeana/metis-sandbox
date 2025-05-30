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

@Configuration
public class MetricsServiceConfig {

  @Bean
  MetricsService metricsService(
      ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository,
      ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository,
      DatasetProblemPatternRepository problemPatternRepository,
      MeterRegistry meterRegistry) {
    return new MetricsService(executionRecordRepository, executionRecordExceptionLogRepository,
        executionRecordWarningExceptionRepository, problemPatternRepository,
        meterRegistry);
  }
}
