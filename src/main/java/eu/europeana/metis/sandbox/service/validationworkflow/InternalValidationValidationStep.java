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

/**
 * The type Internal validation step.
 */
public class InternalValidationValidationStep implements ValidationStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final InternalValidationService internalValidationService;
    private final RecordLogService recordLogService;

    /**
     * Instantiates a new Internal validation  step.
     *
     * @param internalValidationService the internal validation service
     */
    public InternalValidationValidationStep(InternalValidationService internalValidationService,
                                            RecordLogService recordLogService) {
        this.internalValidationService = internalValidationService;
        this.recordLogService = recordLogService;
    }

    @Override
    public ValidationStepContent performStep(Record recordToValidate) {
        ValidationStepContent validationResult;
        try {
            RecordInfo recordInfoValidated = internalValidationService.validate(recordToValidate);
            recordToValidate = ValidatedRecordExtractor.extractRecord(recordInfoValidated);
            LOGGER.info("internal validation step success {}", recordToValidate.getDatasetName());
            validationResult = ValidatedRecordExtractor.extractResults(Step.VALIDATE_INTERNAL, recordInfoValidated);
            recordLogService.logRecordEvent(new RecordProcessEvent(recordInfoValidated, Step.VALIDATE_INTERNAL, Status.SUCCESS));
            recordLogService.logRecordEvent(new RecordProcessEvent(recordInfoValidated, Step.CLOSE, Status.SUCCESS));
        } catch (Exception ex) {
            LOGGER.error("internal validation step fail", ex);
            validationResult = new ValidationStepContent(new ValidationResult(Step.VALIDATE_INTERNAL,
                    new RecordValidationMessage(RecordValidationMessage.Type.ERROR, ex.toString()),
                    ValidationResult.Status.FAILED), recordToValidate);
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.VALIDATE_INTERNAL, Status.FAIL));
        }
        return validationResult;
    }
}
