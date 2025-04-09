package eu.europeana.metis.sandbox.scheduler;

import eu.europeana.metis.sandbox.service.util.XsltUrlUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class XsltUrlUpdateScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(XsltUrlUpdateScheduler.class);

    private final XsltUrlUpdateService xsltUrlUpdateService;

    @Value("${sandbox.transformation.xslt-url}")
    private String defaultXsltUrl;

    public XsltUrlUpdateScheduler(XsltUrlUpdateService xsltUrlUpdateService) {
        this.xsltUrlUpdateService = xsltUrlUpdateService;
    }

    @Scheduled(cron = "${sandbox.transformation.xslt-update-frequency:0 0 * * * *}")
    public void updateDefaultXsltUrl() {
        xsltUrlUpdateService.updateXslt(defaultXsltUrl);
    }

    @EventListener
    public void init(ApplicationReadyEvent readyEvent) {
        LOGGER.info("All beans initialized");
        //Run this on startup so that we have a valid xslt
        updateDefaultXsltUrl();
    }

}
