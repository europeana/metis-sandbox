package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.sandbox.service.metrics.MetricsService;
import eu.europeana.metis.sandbox.service.util.DataCleanupService;
import eu.europeana.metis.sandbox.service.util.XsltUrlUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration class for scheduling tasks and defining beans related to scheduling tasks.
 */
@Configuration
class ScheduledConfig {

  /**
   * Retrieves the transformation XSLT update frequency configuration.
   *
   * @param transformationFrequency the cron expression for update frequency retrieved from the application configuration
   * @return the transformation XSLT update frequency as a string
   */
  @Bean
  public String getTransformationXsltUpdateFrequency(
      @Value("${sandbox.transformation.xslt-update-frequency:0 0 * * * *}") String transformationFrequency) {
    return transformationFrequency;
  }

  /**
   * Retrieves the metrics frequency configuration value.
   *
   * @param metricsFrequency specifies the metrics frequency in cron format; default is used if not provided
   * @return the configured or default metrics frequency
   */
  @Bean
  public String getMetricsFrequency(@Value("${sandbox.metrics.frequency:*/5 * * * * *}") String metricsFrequency) {
    return metricsFrequency;
  }

  /**
   * Retrieves the configured dataset clean frequency cron expression.
   *
   * @param datasetCleanFrequency the cron expression for dataset cleaning schedule, injected from the configuration
   * @return the configured or default cron expression for the dataset cleaning frequency
   */
  @Bean
  public String getDatasetCleanFrequency(@Value("${sandbox.dataset.clean.frequency:0 0 0 * * ?}") String datasetCleanFrequency) {
    return datasetCleanFrequency;
  }

  @Slf4j
  @Configuration
  @EnableScheduling
  static class ScheduledTasks {

    private static final int SCHEDULED_TASK_POOL_SIZE = 5;
    private static final int AWAIT_TERMINATION_SECONDS = 10;
    private final MetricsService metricsService;
    private final XsltUrlUpdateService xsltUrlUpdateService;
    private final DataCleanupService dataCleanupService;
    @Value("${sandbox.transformation.xslt-url}")
    private String defaultXsltUrl;
    @Value("${sandbox.dataset.clean.days-to-preserve:7}")
    private int daysToPreserve;

    ScheduledTasks(MetricsService metricsService, XsltUrlUpdateService xsltUrlUpdateService,
        DataCleanupService dataCleanupService) {
      this.metricsService = metricsService;
      this.xsltUrlUpdateService = xsltUrlUpdateService;
      this.dataCleanupService = dataCleanupService;
    }

    @Bean
    ThreadPoolTaskScheduler taskScheduler() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setPoolSize(SCHEDULED_TASK_POOL_SIZE);
      scheduler.setThreadNamePrefix("Scheduled-");
      scheduler.setWaitForTasksToCompleteOnShutdown(true);
      scheduler.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
      return scheduler;
    }

    @Scheduled(cron = "#{@getMetricsFrequency}")
    void metricsReport() {
      metricsService.generateMetrics();
    }

    @Scheduled(cron = "#{@getTransformationXsltUpdateFrequency}")
    void updateDefaultXsltUrl() {
      xsltUrlUpdateService.updateXslt(defaultXsltUrl);
    }

    @EventListener(ApplicationReadyEvent.class)
    void initializeAfterStartup() {
      updateDefaultXsltUrl();
      metricsReport();
    }

    @Scheduled(cron = "#{@getDatasetCleanFrequency}")
    void remove() {
      log.info("Start daily dataset clean up for last {} days", daysToPreserve);
      dataCleanupService.remove(daysToPreserve);
      log.info("Finish daily dataset clean up");
    }
  }
}
