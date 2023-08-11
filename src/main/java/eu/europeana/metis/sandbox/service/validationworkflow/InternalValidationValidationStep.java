package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.domain.Record;
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
    private final ValidationExtractor validationExtractor;

    /**
     * Instantiates a new Internal validation  step.
     *
     * @param internalValidationService the internal validation service
     */
    public InternalValidationValidationStep(InternalValidationService internalValidationService, ValidationExtractor validationExtractor) {
        this.internalValidationService = internalValidationService;
        this.validationExtractor = validationExtractor;
    }

    @Override
    public void setNextValidationStep(ValidationStep nextValidationStep) {
        // There is no next validation step
    }

    @Override
    public ValidationResult validate(Record recordToValidate) {
        try {
            recordToValidate = validationExtractor.extract(internalValidationService.validate(recordToValidate));
            LOGGER.info("internal validation step success {}", recordToValidate.getDatasetName());
            return new ValidationResult("success", ValidationResult.Status.PASSED);
        } catch (Exception ex) {
            LOGGER.error("internal validation step fail", ex);
            return new ValidationResult("internal validation", ValidationResult.Status.FAILED);
        }

    }
}
