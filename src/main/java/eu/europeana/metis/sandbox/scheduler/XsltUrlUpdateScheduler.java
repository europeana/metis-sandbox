package eu.europeana.metis.sandbox.scheduler;

import eu.europeana.metis.sandbox.service.util.XsltUrlUpdateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class XsltUrlUpdateScheduler {

  private final XsltUrlUpdateService xsltUrlUpdateService;

  @Value("${sandbox.transformation.xslt-url}")
  private String defaultXsltUrl;

  public XsltUrlUpdateScheduler(XsltUrlUpdateService xsltUrlUpdateService) {
    this.xsltUrlUpdateService = xsltUrlUpdateService;
  }

  // "0 0 * * * *" = every hour of every day.
  @Scheduled(cron = "0 0 * * * *")
  public void updateDefaultXsltUrl() {
    xsltUrlUpdateService.updateXslt(defaultXsltUrl);
  }

}
