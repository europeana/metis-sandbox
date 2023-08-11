package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

public class ValidatedRecordExtractor implements ValidationExtractor {

    @Override
    public Record extract(RecordInfo validatedRecordInfo) {
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
}
