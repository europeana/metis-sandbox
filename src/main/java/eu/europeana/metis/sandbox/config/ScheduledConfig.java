package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.sandbox.service.dataset.DataCleanupService;
import eu.europeana.metis.sandbox.service.metrics.MetricsService;
import eu.europeana.metis.sandbox.service.util.XsltUrlUpdateService;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration class for scheduling tasks and defining beans related to scheduling intervals.
 */
@Configuration
public class ScheduledConfig {

  @Configuration
  @EnableScheduling
  static class ScheduledTasks {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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

    @Scheduled(cron = "${sandbox.metrics.frequency:*/5 * * * * *}")
    void metricsReport() {
      metricsService.getDatabaseMetrics();
    }

    @Scheduled(cron = "${sandbox.transformation.xslt-update-frequency:0 0 * * * *}")
    public void updateDefaultXsltUrl() {
      xsltUrlUpdateService.updateXslt(defaultXsltUrl);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeAfterStartup() {
      updateDefaultXsltUrl();
    }

    @Scheduled(cron = "${sandbox.dataset.clean.frequency:0 0 0 * * ?}")
    void remove() {
      LOGGER.info("Start daily dataset clean up for last {} days", daysToPreserve);
      dataCleanupService.remove(daysToPreserve);
      LOGGER.info("Finish daily dataset clean up");
    }
  }

}
