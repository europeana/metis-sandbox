package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.domain.Record;

public class ValidationStepContent {

    private final ValidationResult validationStepResult;
    private final Record recordStepResult;

    public ValidationStepContent(ValidationResult validationStepResult, Record recordStepResult) {
        this.validationStepResult = validationStepResult;
        this.recordStepResult = recordStepResult;
    }

    public ValidationResult getValidationStepResult() {
        return validationStepResult;
    }

    public Record getRecordStepResult() {
        return recordStepResult;
    }
}
