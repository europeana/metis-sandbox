package eu.europeana.metis.sandbox.scheduler;

import eu.europeana.metis.sandbox.service.dataset.DatasetRemoverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Schedule to delete old datasets
 */
@Component
class DatasetRemoverSchedule {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatasetRemoverSchedule.class);

  @Value("${sandbox.dataset.clean.enable}")
  private boolean cleanupEnable;

  @Value("${sandbox.dataset.clean.days-to-preserve:7}")
  private int daysToPreserve;

  private final DatasetRemoverService service;

  DatasetRemoverSchedule(DatasetRemoverService service) {
    this.service = service;
  }

  /**
   * Task to execute on specified frequency or daily if not frequency is specified
   */
  @Scheduled(cron = "${sandbox.dataset.clean.frequency:0 0 0 ? * * *}")
  void remove() {
    if (cleanupEnable) {
      LOGGER.info("Start daily dataset clean up for last {} days", daysToPreserve);
      service.remove(daysToPreserve);
      LOGGER.info("Finish daily dataset clean up");
    } else {
      LOGGER.info("Clean up is not enabled");
    }
  }
}
