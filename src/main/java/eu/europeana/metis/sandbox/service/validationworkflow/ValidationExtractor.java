package eu.europeana.metis.sandbox.service.validationworkflow;

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
    Record extract(RecordInfo validatedRecordInfo);
}
