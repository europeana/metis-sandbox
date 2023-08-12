package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class HarvestValidationStepTest {
    @Mock
    RecordLogService recordLogService;
    @Mock
    ExternalValidationStep externalValidationStep;
    @InjectMocks
    private HarvestValidationStep harvestValidationStep;

    @Test
    void validate_expectSuccess() {
        //given
        doNothing().when(recordLogService).logRecordEvent(any());

        Record record = new Record.RecordBuilder()
                .recordId(1L)
                .datasetId("datasetId")
                .europeanaId("europeanaId")
                .datasetName("datasetName")
                .country(Country.NETHERLANDS)
                .language(Language.NL)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        when(externalValidationStep.validate(any())).thenReturn(List.of(new ValidationResult(Step.VALIDATE_EXTERNAL,
                new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                ValidationResult.Status.PASSED)));
        harvestValidationStep.setNextValidationStep(externalValidationStep);

        //when
        List<ValidationResult> validationResults = harvestValidationStep.validate(record);

        //then
        Optional<ValidationResult> result = validationResults.stream().filter(f -> f.getStep().equals(Step.HARVEST_FILE)).findFirst();
        assertTrue(result.isPresent());
        assertEquals(ValidationResult.Status.PASSED, result.get().getStatus());
        Optional<RecordValidationMessage> message = result.get().getMessages().stream().findFirst();
        assertTrue(message.isPresent());
        assertEquals("success", message.get().getMessage());
        assertEquals(RecordValidationMessage.Type.INFO, message.get().getMessageType());
        verify(recordLogService, times(1)).logRecordEvent( any());
    }

    @Test
    void validate_expectFail() {
        //given
        doNothing().when(recordLogService).logRecordEvent(any());
        doThrow(new RuntimeException("Validation error")).when(externalValidationStep).validate(any());

        Record record = new Record.RecordBuilder()
                .recordId(1L)
                .datasetId("datasetId")
                .europeanaId("europeanaId")
                .datasetName("datasetName")
                .country(Country.NETHERLANDS)
                .language(Language.NL)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        harvestValidationStep.setNextValidationStep(externalValidationStep);

        //when
        List<ValidationResult> validationResults = harvestValidationStep.validate(record);

        //then
        Optional<ValidationResult> result = validationResults.stream().filter(f -> f.getStep().equals(Step.HARVEST_FILE)).findFirst();
        assertTrue(result.isPresent());
        assertEquals(ValidationResult.Status.FAILED, result.get().getStatus());
        Optional<RecordValidationMessage> message = result.get().getMessages().stream().findFirst();
        assertTrue(message.isPresent());
        assertEquals("java.lang.RuntimeException: Validation error", message.get().getMessage());
        assertEquals(RecordValidationMessage.Type.ERROR, message.get().getMessageType());
        verify(recordLogService, times(1)).logRecordEvent(any());
    }
}
