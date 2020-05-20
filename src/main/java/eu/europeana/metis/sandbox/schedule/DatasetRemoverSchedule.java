package eu.europeana.metis.sandbox.schedule;

import eu.europeana.metis.sandbox.service.dataset.DatasetRemoverService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class DatasetRemoverSchedule {

  @Value("${sandbox.dataset.days-to-preserve}")
  private int daysToPreserve;

  private final DatasetRemoverService service;

  DatasetRemoverSchedule(DatasetRemoverService service) {
    this.service = service;
  }

  //At 00:00:00am every day
  @Scheduled(cron = "0 0 0 ? * * *")
  void remove() {
    service.remove(daysToPreserve);
  }
}
