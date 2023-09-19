package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.workflow.ExternalValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * The type External validation step.
 */
public class ExternalValidationStep implements ValidationStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ExternalValidationService externalValidationService;
    private final ValidationExtractor validationExtractor;
    private final RecordLogService recordLogService;

    /**
     * Instantiates a new External validation step.
     *
     * @param externalValidationService the external validation service
     */
    public ExternalValidationStep(ExternalValidationService externalValidationService,
                                  ValidationExtractor validationExtractor,
                                  RecordLogService recordLogService) {
        this.externalValidationService = externalValidationService;
        this.validationExtractor = validationExtractor;
        this.recordLogService = recordLogService;
    }

    @Override
    public ValidationStepContent performStep(Record recordToValidate) {
        ValidationStepContent validationResult;
        try {
            RecordInfo recordInfoValidated = externalValidationService.validate(recordToValidate);
            validationResult = validationExtractor.extractResults(Step.VALIDATE_EXTERNAL, recordInfoValidated);
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.VALIDATE_EXTERNAL, Status.SUCCESS));
        } catch (Exception ex) {
            LOGGER.error("external validation step fail", ex);
            validationResult = new ValidationStepContent(new ValidationResult(Step.VALIDATE_EXTERNAL,
                    new RecordValidationMessage(RecordValidationMessage.Type.ERROR, ex.toString()),
                    ValidationResult.Status.FAILED), recordToValidate);
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.VALIDATE_EXTERNAL, Status.FAIL));
        }
        return validationResult;
    }
}
