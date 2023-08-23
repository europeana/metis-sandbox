package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.domain.Record;

import java.util.List;

/**
 * The interface Validation step.
 */
public interface ValidationStep {
    /**
     * Sets next validation step.
     *
     * @param nextValidationStep the next validation step
     */
    void setNextValidationStep(ValidationStep nextValidationStep);

    /**
     * Performs the step to the given list.
     *
     * @param recordToValidate the record to validate
     * @return the list
     */
    List<ValidationResult> performStep(Record recordToValidate);
}
