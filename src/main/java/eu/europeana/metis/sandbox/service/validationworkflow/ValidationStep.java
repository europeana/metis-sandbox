package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.domain.Record;

public interface ValidationStep {
    void setNextValidationStep(ValidationStep nextValidationStep);
    ValidationResult validate(Record recordToValidate);
}
