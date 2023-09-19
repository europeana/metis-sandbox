package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ValidatedRecordExtractorTest {
    private final ValidatedRecordExtractor validatedRecordExtractor = new ValidatedRecordExtractor();

    @Test
    void extractRecord_expectSuccess() {
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
        Record extractedRecord = validatedRecordExtractor.extractRecord(recordInfo);

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

    @Test
    void extractResults_expectSuccess() {
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
        ValidationStepContent validationResults = validatedRecordExtractor.extractResults(Step.HARVEST_FILE, recordInfo);

        // then
        ValidationResult result = validationResults.getValidationStepResult();
        assertNotNull(result);
        assertEquals(ValidationResult.Status.PASSED, result.getStatus());
        Optional<RecordValidationMessage> message = result.getMessages().stream().findFirst();
        assertEquals("success", message.get().getMessage());
    }

    @Test
    void extractResults_withErrors_expectSuccess() {
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
        RecordInfo recordInfo = new RecordInfo(expectedRecord, List.of(new RecordError("Fail message1", "stackTrace1"),
                new RecordError("Fail message2", "stackTrace2")));
        // when
        ValidationStepContent validationResults = validatedRecordExtractor.extractResults(Step.HARVEST_FILE, recordInfo);

        // then
        ValidationResult result = validationResults.getValidationStepResult();
        assertEquals(ValidationResult.Status.FAILED, result.getStatus());
        Optional<RecordValidationMessage> message = result.getMessages().stream().findFirst();
        assertEquals("Fail message1", message.get().getMessage());
    }
}
