package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidatedRecordExtractorTest {
    private final ValidatedRecordExtractor validatedRecordExtractor = new ValidatedRecordExtractor();

    @Test
    void extract_expectSuccess() {
        // given
        Record expectedRecord = new Record.RecordBuilder()
                .recordId(1L)
                .country(Country.NETHERLANDS)
                .language(Language.NL)
                .datasetId("datasetId")
                .datasetName("datasetName")
                .europeanaId("europeanaId")
                .providerId("providerId")
                .content("content".getBytes(StandardCharsets.UTF_8))
                .build();
        RecordInfo recordInfo = new RecordInfo(expectedRecord);

        // when
        Record extractedRecord = validatedRecordExtractor.extract(recordInfo);

        // then
        assertEquals(expectedRecord.getRecordId(), extractedRecord.getRecordId());
        assertEquals(expectedRecord.getCountry(), extractedRecord.getCountry());
        assertEquals(expectedRecord.getLanguage(), extractedRecord.getLanguage());
        assertEquals(expectedRecord.getDatasetId(), extractedRecord.getDatasetId());
        assertEquals(expectedRecord.getDatasetName(), extractedRecord.getDatasetName());
        assertEquals(expectedRecord.getEuropeanaId(), extractedRecord.getEuropeanaId());
        assertEquals(expectedRecord.getProviderId(), extractedRecord.getProviderId());
        assertArrayEquals(expectedRecord.getContent(), extractedRecord.getContent());
    }
}
