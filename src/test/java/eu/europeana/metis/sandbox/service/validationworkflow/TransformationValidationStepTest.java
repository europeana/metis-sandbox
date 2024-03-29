package eu.europeana.metis.sandbox.service.validationworkflow;


import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.workflow.TransformationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransformationValidationStepTest {
    @Mock
    TransformationService transformationService;
    @Mock
    RecordLogService recordLogService;
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
        when(transformationService.transformToEdmInternal(any())).thenReturn(recordInfo);

        //when
        ValidationStepContent validationStepContent = transformationValidationStep.performStep(recordToValidate);

        //then
        ValidationResult result = validationStepContent.getValidationStepResult();
        assertNotNull(result);
        assertEquals(ValidationResult.Status.PASSED, result.getStatus());
        Optional<RecordValidationMessage> message = result.getMessages().stream().findFirst();
        assertTrue(message.isPresent());
        assertEquals("success", message.get().getMessage());
        assertEquals(RecordValidationMessage.Type.INFO, message.get().getMessageType());
        verify(transformationService, times(1)).transformToEdmInternal(any());
        verify(recordLogService, times(1)).logRecordEvent(any());

    }

    @Test
    void validate_expectFail() {
        //given
        Record recordToValidate = new Record.RecordBuilder()
                .language(Language.NL)
                .country(Country.NETHERLANDS)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        when(transformationService.transformToEdmInternal(any())).thenThrow(new RuntimeException("Transformation exception"));

        //when
        ValidationStepContent validationStepContent = transformationValidationStep.performStep(recordToValidate);

        //then
        ValidationResult result = validationStepContent.getValidationStepResult();
        assertNotNull(result);
        assertEquals(ValidationResult.Status.FAILED, result.getStatus());
        Optional<RecordValidationMessage> message = result.getMessages().stream().findFirst();
        assertTrue(message.isPresent());
        assertEquals("java.lang.RuntimeException: Transformation exception", message.get().getMessage());
        assertEquals(RecordValidationMessage.Type.ERROR, message.get().getMessageType());
        verify(transformationService, times(1)).transformToEdmInternal(any());
        verify(recordLogService, times(1)).logRecordEvent(any());
    }
}
