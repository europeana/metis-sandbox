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

/**
 * The type Transformation validation step.
 */
public class TransformationValidationStep implements ValidationStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final TransformationService transformationService;
    private final RecordLogService recordLogService;

    /**
     * Instantiates a new Transformation validation step.
     *
     * @param transformationService the transformation service
     */
    public TransformationValidationStep(TransformationService transformationService,
                                        RecordLogService recordLogService) {
        this.transformationService = transformationService;
        this.recordLogService = recordLogService;
    }

    @Override
    public ValidationStepContent performStep(Record recordToValidate) {
        ValidationStepContent validationStepContent;
        try {
            RecordInfo recordInfoValidated = transformationService.transformToEdmInternal(recordToValidate);
            validationStepContent = ValidatedRecordExtractor.extractValidationStepContent(Step.TRANSFORM_INTERNAL, recordInfoValidated);
            recordLogService.logRecordEvent(new RecordProcessEvent(recordInfoValidated, Step.TRANSFORM_INTERNAL, Status.SUCCESS));
        } catch (Exception ex) {
            LOGGER.error("transformation validation step fail", ex);
            validationStepContent = new ValidationStepContent(new ValidationResult(Step.TRANSFORM_INTERNAL,
                    new RecordValidationMessage(RecordValidationMessage.Type.ERROR, ex.toString()),
                    ValidationResult.Status.FAILED), recordToValidate);
            recordLogService.logRecordEvent(new RecordProcessEvent(new RecordInfo(recordToValidate), Step.TRANSFORM_INTERNAL, Status.FAIL));
        }
        return validationStepContent;
    }
}
