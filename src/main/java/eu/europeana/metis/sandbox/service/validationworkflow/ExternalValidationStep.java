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
import java.util.ArrayList;
import java.util.List;

/**
 * The type External validation step.
 */
public class ExternalValidationStep implements ValidationStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ExternalValidationService externalValidationService;
    private final ValidationExtractor validationExtractor;
    private ValidationStep nextValidationStep;
    private RecordLogService recordLogService;

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
    public void setNextValidationStep(ValidationStep nextValidationStep) {
        this.nextValidationStep = nextValidationStep;
    }

    @Override
    public List<ValidationResult> validate(Record recordToValidate) {
        List<ValidationResult> validationResults = new ArrayList<>();
        try {
            RecordInfo recordInfoValidated = externalValidationService.validate(recordToValidate);
            validationResults.addAll(validationExtractor.extractResults(Step.VALIDATE_EXTERNAL,
                    recordInfoValidated,
                    this.nextValidationStep.validate(validationExtractor.extractRecord(recordInfoValidated))));
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.VALIDATE_EXTERNAL, Status.SUCCESS));
        } catch (Exception ex) {
            LOGGER.error("external validation step fail", ex);
            validationResults.removeIf(validationResult -> validationResult.getStep().equals(Step.VALIDATE_EXTERNAL));
            validationResults.add(new ValidationResult(Step.VALIDATE_EXTERNAL,
                    new RecordValidationMessage(RecordValidationMessage.Type.ERROR, ex.toString()),
                    ValidationResult.Status.FAILED));
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.VALIDATE_EXTERNAL, Status.FAIL));
        }
        return validationResults;
    }
}
