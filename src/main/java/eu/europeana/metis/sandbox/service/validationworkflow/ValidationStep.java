package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.domain.Record;

/**
 * The interface Validation step.
 */
public interface ValidationStep {

    /**
     * Performs the step to the given list.
     *
     * @param recordToValidate the record to validate
     * @return the list
     */
    ValidationStepContent performStep(Record recordToValidate);
}
