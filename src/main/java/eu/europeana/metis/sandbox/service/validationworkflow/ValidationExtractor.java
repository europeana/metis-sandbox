package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

/**
 * The interface Validation extractor.
 */
public interface ValidationExtractor {
    /**
     * Extract record.
     *
     * @param validatedRecordInfo the validated record info
     * @return the record
     */
    Record extractRecord(RecordInfo validatedRecordInfo);

    /**
     * Extract results list.
     *
     * @param step              the step
     * @param recordInfo        the record info
     * @return the list
     */
    ValidationStepContent extractResults(Step step,
                                          RecordInfo recordInfo);
}
