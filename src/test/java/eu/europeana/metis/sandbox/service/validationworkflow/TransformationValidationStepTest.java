package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.workflow.TransformationService;
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
class TransformationValidationStepTest {
    @Mock
    TransformationService transformationService;

    @Mock
    InternalValidationValidationStep internalValidationValidationStep;

    @Mock
    ValidationExtractor validationExtractor;

    @InjectMocks
    TransformationValidationStep transformationValidationStep;

    @Test
    void validate_expectSuccess() {
        //given
        Record recordToValidate = new Record.RecordBuilder()
                .language(Language.NL)
                .country(Country.NETHERLANDS)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        Record transformedRecord = new Record.RecordBuilder()
                .language(Language.NL)
                .country(Country.NETHERLANDS)
                .datasetName("datasetName")
                .datasetId("datasetId")
                .europeanaId("europeanaId")
                .providerId("providerId")
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        RecordInfo recordInfo = new RecordInfo(transformedRecord);
        when(transformationService.transform(any())).thenReturn(recordInfo);
        when(validationExtractor.extract(any())).thenReturn(recordInfo.getRecord());
        when(internalValidationValidationStep.validate(any())).thenReturn(new ValidationResult("success", ValidationResult.Status.PASSED));
        transformationValidationStep.setNextValidationStep(internalValidationValidationStep);

        //when
        ValidationResult validationResult = transformationValidationStep.validate(recordToValidate);

        //then
        assertEquals("success", validationResult.getMessage());
        assertEquals(ValidationResult.Status.PASSED, validationResult.getStatus());
        verify(transformationService, times(1)).transform(any());
    }

    @Test
    void validate_expectFail() {
        //given
        Record recordToValidate = new Record.RecordBuilder()
                .language(Language.NL)
                .country(Country.NETHERLANDS)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        when(transformationService.transform(any())).thenThrow(new RuntimeException("Transformation exception"));

        //when
        ValidationResult validationResult = transformationValidationStep.validate(recordToValidate);

        //then
        assertEquals("Transformation", validationResult.getMessage());
        assertEquals(ValidationResult.Status.FAILED, validationResult.getStatus());
        verify(transformationService, times(1)).transform(any());
    }
}
