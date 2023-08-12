package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.domain.Record;

import java.util.List;

public interface ValidationStep {
    void setNextValidationStep(ValidationStep nextValidationStep);
    List<ValidationResult> validate(Record recordToValidate);
}
