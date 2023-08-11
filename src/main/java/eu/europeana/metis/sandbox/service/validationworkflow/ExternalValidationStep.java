package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.domain.Record;
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
    private ValidationStep nextValidationStep;

    /**
     * Instantiates a new External validation step.
     *
     * @param externalValidationService the external validation service
     */
    public ExternalValidationStep(ExternalValidationService externalValidationService, ValidationExtractor validationExtractor) {
        this.externalValidationService = externalValidationService;
        this.validationExtractor = validationExtractor;
    }

    @Override
    public void setNextValidationStep(ValidationStep nextValidationStep) {
        this.nextValidationStep = nextValidationStep;
    }

    @Override
    public ValidationResult validate(Record recordToValidate) {
        try {
            recordToValidate = validationExtractor.extract(externalValidationService.validate(recordToValidate));
            LOGGER.info("external validation step success {}", recordToValidate.getDatasetName());
            return this.nextValidationStep.validate(recordToValidate);
        } catch (Exception ex) {
            LOGGER.error("external validation step fail", ex);
            return new ValidationResult("external validation", ValidationResult.Status.FAILED);
        }
    }
}
