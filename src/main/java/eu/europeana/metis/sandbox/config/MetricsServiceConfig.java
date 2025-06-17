package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningExceptionRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.service.metrics.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the metrics service.
 */
@Configuration
class MetricsServiceConfig {

  @Bean
  MetricsService metricsService(
      ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordExceptionRepository executionRecordExceptionRepository,
      ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository,
      DatasetProblemPatternRepository problemPatternRepository,
      MeterRegistry meterRegistry) {
    return new MetricsService(executionRecordRepository, executionRecordExceptionRepository,
        executionRecordWarningExceptionRepository, problemPatternRepository,
        meterRegistry);
  }
}
