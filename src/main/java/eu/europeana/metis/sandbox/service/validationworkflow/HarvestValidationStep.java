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
import java.util.ArrayList;
import java.util.List;

/**
 * The type Harvest validation step.
 */
public class HarvestValidationStep implements ValidationStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final RecordLogService recordLogService;
    private ValidationStep nextValidationStep;

    /**
     * Instantiates a new Harvest validation step.
     *
     * @param recordLogService the record log service
     */
    public HarvestValidationStep(RecordLogService recordLogService) {
        this.recordLogService = recordLogService;
    }

    @Override
    public void setNextValidationStep(ValidationStep nextValidationStep) {
        this.nextValidationStep = nextValidationStep;
    }

    @Override
    public List<ValidationResult> validate(Record recordToValidate) {
        List<ValidationResult> validationResults = new ArrayList<>();
        try {
            LOGGER.info("harvesting validation step virtual dataset {}", recordToValidate.getDatasetName());
            validationResults.add(new ValidationResult(Step.HARVEST_FILE,
                    new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                    ValidationResult.Status.PASSED));
            validationResults.addAll(this.nextValidationStep.validate(recordToValidate));
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.HARVEST_FILE, Status.SUCCESS));
        } catch (Exception ex) {
            LOGGER.error("harvesting validation step fail", ex);
            validationResults.removeIf(validationResult -> validationResult.getStep().equals(Step.HARVEST_FILE));
            validationResults.add(new ValidationResult(Step.HARVEST_FILE,
                    new RecordValidationMessage(RecordValidationMessage.Type.ERROR, ex.toString()),
                    ValidationResult.Status.FAILED));
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.HARVEST_FILE, Status.FAIL));
        }
        return validationResults;
    }
}
