package eu.europeana.metis.sandbox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ServicesConfig {

//  private MetricsService metricsService;
//
//  @Bean
//  MetricsService metricsService(
//      RecordRepository recordRepository,
//      RecordLogRepository recordLogRepository,
//      DatasetProblemPatternRepository problemPatternRepository,
//      MeterRegistry meterRegistry) {
//    metricsService = new MetricsServiceImpl(recordRepository, recordLogRepository, problemPatternRepository,
//            meterRegistry, amqpConfiguration);
//    return metricsService;
//  }
//
//  @Scheduled(cron = "${sandbox.metrics.frequency:*/5 * * * * *}")
//  void metricsReport() {
//    metricsService.processMetrics();
//  }
}
