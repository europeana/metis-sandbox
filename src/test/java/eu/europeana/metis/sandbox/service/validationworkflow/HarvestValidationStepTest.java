package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class HarvestValidationStepTest {
    @Mock
    DatasetService datasetService;
    @Mock
    RecordRepository recordRepository;
    @Mock
    ExternalValidationStep externalValidationStep;

    @InjectMocks
    private HarvestValidationStep harvestValidationStep;

    @Test
    void validate_expectSuccess() {
        //given
        RecordEntity recordEntity = new RecordEntity("providerId", "datasetId");
        when(datasetService.createEmptyDataset(anyString(), any(), any(), any())).thenReturn("datasetId");
        when(recordRepository.save(any())).thenReturn(recordEntity);
        Record record = new Record.RecordBuilder()
                .language(Language.NL)
                .country(Country.NETHERLANDS)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        when(externalValidationStep.validate(any())).thenReturn(new ValidationResult("success", ValidationResult.Status.PASSED));
        harvestValidationStep.setNextValidationStep(externalValidationStep);

        //when
        ValidationResult validationResult = harvestValidationStep.validate(record);

        //then
        assertEquals("success", validationResult.getMessage());
        assertEquals(ValidationResult.Status.PASSED, validationResult.getStatus());
        verify(datasetService, times(1)).createEmptyDataset(anyString(), any(), any(), any());
        verify(recordRepository, times(1)).save(any());
    }

    @Test
    void validate_expectFail() {
        //given
        when(datasetService.createEmptyDataset(anyString(), any(), any(), any())).thenReturn("datasetId");
        when(recordRepository.save(any())).thenThrow(new RuntimeException("Harvesting error"));
        Record record = new Record.RecordBuilder()
                .language(Language.NL)
                .country(Country.NETHERLANDS)
                .content("info".getBytes(StandardCharsets.UTF_8))
                .build();
        harvestValidationStep.setNextValidationStep(externalValidationStep);

        //when
        ValidationResult validationResult = harvestValidationStep.validate(record);

        //then
        assertEquals("harvest", validationResult.getMessage());
        assertEquals(ValidationResult.Status.FAILED, validationResult.getStatus());
        verify(datasetService, times(1)).createEmptyDataset(anyString(), any(), any(), any());
        verify(recordRepository, times(1)).save(any());
    }
}
