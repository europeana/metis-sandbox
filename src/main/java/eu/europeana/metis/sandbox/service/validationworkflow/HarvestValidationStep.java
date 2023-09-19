package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * The type Harvest validation step.
 */
public class HarvestValidationStep implements ValidationStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final RecordLogService recordLogService;

    /**
     * Instantiates a new Harvest validation step.
     *
     * @param recordLogService the record log service
     */
    public HarvestValidationStep(RecordLogService recordLogService) {
        this.recordLogService = recordLogService;
    }

    @Override
    public ValidationStepContent performStep(Record recordToValidate) {
        ValidationResult validationResult;
        try {
            LOGGER.info("harvesting validation step virtual dataset {}", recordToValidate.getDatasetName());

            validationResult = new ValidationResult(Step.HARVEST_FILE,
                    new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                    ValidationResult.Status.PASSED);

            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.HARVEST_FILE, Status.SUCCESS));

        } catch (Exception ex) {
            LOGGER.error("harvesting validation step fail", ex);
            validationResult = new ValidationResult(Step.HARVEST_FILE,
                    new RecordValidationMessage(RecordValidationMessage.Type.ERROR, ex.toString()),
                    ValidationResult.Status.FAILED);
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.HARVEST_FILE, Status.FAIL));
        }
        return new ValidationStepContent(validationResult, recordToValidate);
    }
}
