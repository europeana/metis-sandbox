package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class HarvestValidationStepTest {
    @Mock
    RecordLogService recordLogService;
    @InjectMocks
    private HarvestValidationStep harvestValidationStep;

    @Test
    void validate_expectSuccess() {
        //given
        doNothing().when(recordLogService).logRecordEvent(any());

        Record testRecord = new Record.RecordBuilder()
                .recordId(1L)
                .datasetId("datasetId")
                .europeanaId("europeanaId")
                .datasetName("datasetName")
                .country(Country.NETHERLANDS)
                .language(Language.NL)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();

        //when
        ValidationStepContent validationStepContent = harvestValidationStep.performStep(testRecord);

        //then
        ValidationResult result = validationStepContent.getValidationStepResult();
        assertNotNull(result);
        assertEquals(ValidationResult.Status.PASSED, result.getStatus());
        Optional<RecordValidationMessage> message = result.getMessages().stream().findFirst();
        assertTrue(message.isPresent());
        assertEquals("success", message.get().getMessage());
        assertEquals(RecordValidationMessage.Type.INFO, message.get().getMessageType());
        verify(recordLogService, times(1)).logRecordEvent(any());
    }

    @Test
    void validate_expectFail() {
        //given
        doThrow(new RuntimeException("Validation error")).doNothing().when(recordLogService).logRecordEvent(any());

        Record testRecord = new Record.RecordBuilder()
                .recordId(1L)
                .datasetId("datasetId")
                .europeanaId("europeanaId")
                .datasetName("datasetName")
                .country(Country.NETHERLANDS)
                .language(Language.NL)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();

        //when
        ValidationStepContent validationStepContent = harvestValidationStep.performStep(testRecord);

        //then
        ValidationResult result = validationStepContent.getValidationStepResult();
        assertNotNull(result);
        assertEquals(ValidationResult.Status.FAILED, result.getStatus());
        Optional<RecordValidationMessage> message = result.getMessages().stream().findFirst();
        assertTrue(message.isPresent());
        assertEquals("java.lang.RuntimeException: Validation error", message.get().getMessage());
        assertEquals(RecordValidationMessage.Type.ERROR, message.get().getMessageType());
        verify(recordLogService, times(2)).logRecordEvent(any());
    }
}
