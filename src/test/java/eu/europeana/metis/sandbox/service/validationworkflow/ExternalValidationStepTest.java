package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.workflow.ExternalValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalValidationStepTest {
    @Mock
    ExternalValidationService externalValidationService;
    @Mock
    TransformationValidationStep transformationValidationStep;
    @Mock
    ValidationExtractor validationExtractor;
    @InjectMocks
    ExternalValidationStep externalValidationStep;

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
        when(externalValidationService.validate(any())).thenReturn(recordInfo);
        when(validationExtractor.extract(any())).thenReturn(recordInfo.getRecord());
        when(transformationValidationStep.validate(any())).thenReturn(new ValidationResult("success", ValidationResult.Status.PASSED));
        externalValidationStep.setNextValidationStep(transformationValidationStep);

        //when
        ValidationResult validationResult = externalValidationStep.validate(recordToValidate);

        //then
        assertEquals("success", validationResult.getMessage());
        assertEquals(ValidationResult.Status.PASSED, validationResult.getStatus());
        verify(externalValidationService, times(1)).validate(any());
    }

    @Test
    void validate_expectFail() {
        //given
        Record.RecordBuilder recordBuilder = new Record.RecordBuilder();
        Record recordToValidate = recordBuilder.language(Language.NL)
                .country(Country.NETHERLANDS)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        when(externalValidationService.validate(any())).thenThrow(new RuntimeException("External Validation Failure"));
        externalValidationStep.setNextValidationStep(transformationValidationStep);

        //when
        ValidationResult validationResult = externalValidationStep.validate(recordToValidate);

        //then
        assertEquals("external validation", validationResult.getMessage());
        assertEquals(ValidationResult.Status.FAILED, validationResult.getStatus());
        verify(externalValidationService, times(1)).validate(any());
    }
}
