package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.workflow.InternalValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalValidationValidationStepTest {
    @Mock
    ValidationExtractor validationExtractor;
    @Mock
    InternalValidationService internalValidationService;
    @Mock
    RecordLogService recordLogService;
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
        when(validationExtractor.extractRecord(any())).thenReturn(recordInfo.getRecord());
        when(validationExtractor.extractResults(any(), any(), any())).thenReturn(
                List.of(new ValidationResult(Step.VALIDATE_INTERNAL,
                        new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                        ValidationResult.Status.PASSED))
        );
        doNothing().when(recordLogService).logRecordEvent(any());
        //when
        List<ValidationResult> validationResults = internalValidationValidationStep.validate(recordToValidate);

        //then
        Optional<ValidationResult> result = validationResults.stream().filter(f -> f.getStep().equals(Step.VALIDATE_INTERNAL)).findFirst();
        assertTrue(result.isPresent());
        assertEquals(ValidationResult.Status.PASSED, result.get().getStatus());
        Optional<RecordValidationMessage> message = result.get().getMessages().stream().findFirst();
        assertTrue(message.isPresent());
        assertEquals("success", message.get().getMessage());
        assertEquals(RecordValidationMessage.Type.INFO, message.get().getMessageType());
        verify(internalValidationService, times(1)).validate(any());
        verify(recordLogService, times(2)).logRecordEvent(any());
    }

    @Test
    void validate_expectFail() {
        //given
        Record recordToValidate = new Record.RecordBuilder().language(Language.NL)
                .country(Country.NETHERLANDS)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        when(internalValidationService.validate(any())).thenThrow(new RuntimeException("Internal validation error"));
        doNothing().when(recordLogService).logRecordEvent(any());
        //when
        List<ValidationResult> validationResults = internalValidationValidationStep.validate(recordToValidate);

        //then
        Optional<ValidationResult> result = validationResults.stream().filter(f -> f.getStep().equals(Step.VALIDATE_INTERNAL)).findFirst();
        assertTrue(result.isPresent());
        assertEquals(ValidationResult.Status.FAILED, result.get().getStatus());
        Optional<RecordValidationMessage> message = result.get().getMessages().stream().findFirst();
        assertTrue(message.isPresent());
        assertEquals("java.lang.RuntimeException: Internal validation error", message.get().getMessage());
        assertEquals(RecordValidationMessage.Type.ERROR, message.get().getMessageType());
        verify(internalValidationService, times(1)).validate(any());
        verify(recordLogService, times(1)).logRecordEvent(any());
    }
}
