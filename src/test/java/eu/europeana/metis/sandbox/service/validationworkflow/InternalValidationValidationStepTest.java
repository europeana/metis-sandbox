package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.workflow.InternalValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalValidationValidationStepTest {
    @Mock
    ValidationExtractor validationExtractor;

    @Mock
    InternalValidationService internalValidationService;

    @InjectMocks
    InternalValidationValidationStep internalValidationValidationStep;

    @Test
    void validate_expectSuccess() {
        //given
        Record.RecordBuilder recordBuilder = new Record.RecordBuilder();
        Record recordToValidate = recordBuilder.language(Language.NL)
                .country(Country.NETHERLANDS)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        Record record = recordBuilder.language(Language.NL)
                .country(Country.NETHERLANDS)
                .datasetName("datasetName")
                .datasetId("datasetId")
                .europeanaId("europeanaId")
                .providerId("providerId")
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        RecordInfo recordInfo = new RecordInfo(record);
        when(internalValidationService.validate(any())).thenReturn(recordInfo);
        when(validationExtractor.extract(any())).thenReturn(recordInfo.getRecord());

        //when
        ValidationResult validationResult = internalValidationValidationStep.validate(recordToValidate);

        //then
        assertEquals("success", validationResult.getMessage());
        assertEquals(ValidationResult.Status.PASSED, validationResult.getStatus());
        verify(internalValidationService, times(1)).validate(any());
    }

    @Test
    void validate_expectFail() {
        //given
        Record recordToValidate = new Record.RecordBuilder().language(Language.NL)
                .country(Country.NETHERLANDS)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        when(internalValidationService.validate(any())).thenThrow(new RuntimeException("Internal validation error"));

        //when
        ValidationResult validationResult = internalValidationValidationStep.validate(recordToValidate);

        //then
        assertEquals("internal validation", validationResult.getMessage());
        assertEquals(ValidationResult.Status.FAILED, validationResult.getStatus());
        verify(internalValidationService, times(1)).validate(any());
    }
}
