package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.workflow.ExternalValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalValidationStepTest {
    @Mock
    ExternalValidationService externalValidationService;
    @Mock
    RecordLogService recordLogService;
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
        doNothing().when(recordLogService).logRecordEvent(any());
        when(externalValidationService.validate(any())).thenReturn(recordInfo);

        //when
        ValidationStepContent validationStepContent = externalValidationStep.performStep(recordToValidate);

        //then
        ValidationResult result = validationStepContent.getValidationStepResult();
        assertNotNull(result);
        assertEquals(ValidationResult.Status.PASSED, result.getStatus());
        Optional<RecordValidationMessage> message = result.getMessages().stream().findFirst();
        assertTrue(message.isPresent());
        assertEquals("success", message.get().getMessage());
        assertEquals(RecordValidationMessage.Type.INFO, message.get().getMessageType());
        verify(externalValidationService, times(1)).validate(any());
        verify(recordLogService, times(1)).logRecordEvent(any());

    }

    @Test
    void validate_expectFail() {
        //given
        Record.RecordBuilder recordBuilder = new Record.RecordBuilder();
        Record recordToValidate = recordBuilder.language(Language.NL)
                .country(Country.NETHERLANDS)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        when(externalValidationService.validate(any())).thenThrow(new RuntimeException("External validation failure"));

        //when
        ValidationStepContent validationResults = externalValidationStep.performStep(recordToValidate);

        //then
        ValidationResult result = validationResults.getValidationStepResult();
        assertNotNull(result);
        assertEquals(ValidationResult.Status.FAILED, result.getStatus());
        Optional<RecordValidationMessage> message = result.getMessages().stream().findFirst();
        assertTrue(message.isPresent());
        assertEquals("java.lang.RuntimeException: External validation failure", message.get().getMessage());
        assertEquals(RecordValidationMessage.Type.ERROR, message.get().getMessageType());
        verify(externalValidationService, times(1)).validate(any());
        verify(recordLogService, times(1)).logRecordEvent(any());
    }
}
