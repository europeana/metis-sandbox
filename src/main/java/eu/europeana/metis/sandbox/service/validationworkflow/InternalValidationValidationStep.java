package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.workflow.InternalValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Internal validation step.
 */
public class InternalValidationValidationStep implements ValidationStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final InternalValidationService internalValidationService;
    private final ValidationExtractor validationExtractor;
    private final RecordLogService recordLogService;

    /**
     * Instantiates a new Internal validation  step.
     *
     * @param internalValidationService the internal validation service
     */
    public InternalValidationValidationStep(InternalValidationService internalValidationService,
                                            ValidationExtractor validationExtractor,
                                            RecordLogService recordLogService) {
        this.internalValidationService = internalValidationService;
        this.validationExtractor = validationExtractor;
        this.recordLogService = recordLogService;
    }

    @Override
    public void setNextValidationStep(ValidationStep nextValidationStep) {
        // There is no next validation step
    }

    @Override
    public List<ValidationResult> performStep(Record recordToValidate) {
        List<ValidationResult> validationResults = new ArrayList<>();
        try {
            RecordInfo recordInfoValidated = internalValidationService.validate(recordToValidate);
            recordToValidate = validationExtractor.extractRecord(recordInfoValidated);
            LOGGER.info("internal validation step success {}", recordToValidate.getDatasetName());
            validationResults.addAll(validationExtractor.extractResults(Step.VALIDATE_INTERNAL,
                    recordInfoValidated,
                    new ArrayList<>()));
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.VALIDATE_INTERNAL, Status.SUCCESS));
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.CLOSE, Status.SUCCESS));
        } catch (Exception ex) {
            LOGGER.error("internal validation step fail", ex);
            validationResults.removeIf(validationResult -> validationResult.getStep().equals(Step.VALIDATE_INTERNAL));
            validationResults.add(new ValidationResult(Step.VALIDATE_INTERNAL,
                    new RecordValidationMessage(RecordValidationMessage.Type.ERROR, ex.toString()),
                    ValidationResult.Status.FAILED));
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.VALIDATE_INTERNAL, Status.FAIL));
        }
        return validationResults;
    }
}
