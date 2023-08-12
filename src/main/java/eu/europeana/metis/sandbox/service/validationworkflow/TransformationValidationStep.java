package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.workflow.TransformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Transformation validation step.
 */
public class TransformationValidationStep implements ValidationStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final TransformationService transformationService;
    private final ValidationExtractor validationExtractor;
    private final RecordLogService recordLogService;
    private ValidationStep nextValidationStep;

    /**
     * Instantiates a new Transformation validation step.
     *
     * @param transformationService the transformation service
     */
    public TransformationValidationStep(TransformationService transformationService,
                                        ValidationExtractor validationExtractor,
                                        RecordLogService recordLogService) {
        this.transformationService = transformationService;
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
            RecordInfo recordInfoValidated = transformationService.transformToEdmInternal(recordToValidate);
            validationResults.addAll(validationExtractor.extractResults(Step.TRANSFORM,
                    recordInfoValidated,
                    this.nextValidationStep.validate(validationExtractor.extractRecord(recordInfoValidated))));
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.TRANSFORM, Status.SUCCESS));
        } catch (Exception ex) {
            LOGGER.error("transformation validation step fail", ex);
            validationResults.removeIf(validationResult -> validationResult.getStep().equals(Step.TRANSFORM));
            validationResults.add(new ValidationResult(Step.TRANSFORM,
                    new RecordValidationMessage(RecordValidationMessage.Type.ERROR, ex.toString()),
                    ValidationResult.Status.FAILED));
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.TRANSFORM, Status.FAIL));
        }
        return validationResults;
    }
}
