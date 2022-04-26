package eu.europeana.metis.sandbox.scheduler;

import eu.europeana.metis.sandbox.service.workflow.InternalValidationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanMappingExecutionTimestamp {

    private final InternalValidationService internalValidationService;

    public CleanMappingExecutionTimestamp(InternalValidationService internalValidationService) {
        this.internalValidationService = internalValidationService;
    }

    @Scheduled(cron = "0 0 1 * * *") //Every day at 1am
    void cleanMapping(){
        internalValidationService.cleanMappingExecutionTimestamp();
    }
}
