package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * The type Validated record extractor.
 */
public class ValidatedRecordExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ValidatedRecordExtractor(){
        throw new IllegalStateException("Validated record utility class");
    }

    /**
     * Extract record.
     *
     * @param validatedRecordInfo the validated record info
     * @return the record
     */
    public static Record extractRecord(RecordInfo validatedRecordInfo) {
        return Record.builder()
                .providerId(validatedRecordInfo.getRecordValue().getProviderId())
                .europeanaId(validatedRecordInfo.getRecordValue().getEuropeanaId())
                .datasetId(validatedRecordInfo.getRecordValue().getDatasetId())
                .datasetName(validatedRecordInfo.getRecordValue().getDatasetName())
                .country(validatedRecordInfo.getRecordValue().getCountry())
                .language(validatedRecordInfo.getRecordValue().getLanguage())
                .content(validatedRecordInfo.getRecordValue().getContent())
                .recordId(validatedRecordInfo.getRecordValue().getRecordId())
                .build();
    }

    /**
     * Extract results list.
     *
     * @param step              the step
     * @param recordInfo        the record info
     * @return the list
     */
    public static ValidationStepContent extractValidationStepContent(Step step, RecordInfo recordInfo) {
        ValidationStepContent result;
        if (recordInfo.getErrors().isEmpty()) {
            result = new ValidationStepContent(new ValidationResult(step,
                    new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                    ValidationResult.Status.PASSED), recordInfo.getRecordValue());
            LOGGER.info("validation step {} success {}", step, recordInfo.getRecordValue().getDatasetName());
        } else {
            result = new ValidationStepContent(new ValidationResult(step, recordInfo.getErrors()
                    .stream()
                    .map(item -> new RecordValidationMessage(RecordValidationMessage.Type.ERROR, item.getMessage()))
                    .toList(), ValidationResult.Status.FAILED), recordInfo.getRecordValue());
        }
        return result;
    }
}
