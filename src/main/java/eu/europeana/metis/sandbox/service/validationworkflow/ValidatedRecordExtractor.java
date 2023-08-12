package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The type Validated record extractor.
 */
public class ValidatedRecordExtractor implements ValidationExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Record extractRecord(RecordInfo validatedRecordInfo) {
        Record.RecordBuilder recordBuilder = new Record.RecordBuilder();
        return recordBuilder
                .providerId(validatedRecordInfo.getRecord().getProviderId())
                .europeanaId(validatedRecordInfo.getRecord().getEuropeanaId())
                .datasetId(validatedRecordInfo.getRecord().getDatasetId())
                .datasetName(validatedRecordInfo.getRecord().getDatasetName())
                .country(validatedRecordInfo.getRecord().getCountry())
                .language(validatedRecordInfo.getRecord().getLanguage())
                .content(validatedRecordInfo.getRecord().getContent())
                .recordId(validatedRecordInfo.getRecord().getRecordId())
                .build();
    }

    @Override
    public List<ValidationResult> extractResults(Step step,
                                                 RecordInfo recordInfo,
                                                 List<ValidationResult> validationResults) {
        if (recordInfo.getErrors().isEmpty()) {
            validationResults.add(new ValidationResult(step,
                    new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                    ValidationResult.Status.PASSED));
            LOGGER.info("validation step {} success {}", step, recordInfo.getRecord().getDatasetName());
        } else {
            validationResults.add(new ValidationResult(step, recordInfo.getErrors()
                    .stream()
                    .map(item -> new RecordValidationMessage(RecordValidationMessage.Type.ERROR, item.getMessage()))
                    .collect(Collectors.toList()), ValidationResult.Status.FAILED));
        }
        return validationResults;
    }
}
