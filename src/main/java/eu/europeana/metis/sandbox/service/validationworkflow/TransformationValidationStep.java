package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.workflow.TransformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * The type Transformation validation step.
 */
public class TransformationValidationStep implements ValidationStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ValidationStep nextValidationStep;
    private final TransformationService transformationService;
    private final ValidationExtractor validationExtractor;
    /**
     * Instantiates a new Transformation validation step.
     *
     * @param transformationService the transformation service
     */
    public TransformationValidationStep(TransformationService transformationService, ValidationExtractor validationExtractor) {
        this.transformationService = transformationService;
        this.validationExtractor = validationExtractor;
    }

    @Override
    public void setNextValidationStep(ValidationStep nextValidationStep) {
        this.nextValidationStep = nextValidationStep;
    }

    @Override
    public ValidationResult validate(Record recordToValidate) {
        try {
            recordToValidate = validationExtractor.extract(transformationService.transform(recordToValidate));
            LOGGER.info("transformation validation step success {}", recordToValidate.getDatasetName());
            return this.nextValidationStep.validate(recordToValidate);
        } catch (Exception ex) {
            LOGGER.error("transformation validation step fail", ex);
            return new ValidationResult("Transformation", ValidationResult.Status.FAILED);
        }
    }
}
