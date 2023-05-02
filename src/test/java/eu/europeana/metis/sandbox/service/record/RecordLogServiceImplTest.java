package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordLogServiceImplTest {

    @Mock
    RecordErrorLogRepository errorLogRepository;
    @Mock
    RecordRepository recordRepository;
    @Mock
    private RecordLogRepository recordLogRepository;
    @InjectMocks
    private RecordLogServiceImpl service;

    private static Stream<Arguments> steps() {
        return Stream.of(
                arguments(null, Set.of(Step.HARVEST_ZIP, Step.HARVEST_OAI_PMH)),
                arguments("", Set.of(Step.HARVEST_ZIP, Step.HARVEST_OAI_PMH)),
                arguments("HARVEST", Set.of(Step.HARVEST_ZIP, Step.HARVEST_OAI_PMH)),
                arguments("TRANSFORM_TO_EDM_EXTERNAL", Set.of(Step.TRANSFORM_TO_EDM_EXTERNAL)),
                arguments("VALIDATE_EXTERNAL", Set.of(Step.VALIDATE_EXTERNAL)),
                arguments("TRANSFORM", Set.of(Step.TRANSFORM)),
                arguments("VALIDATE_INTERNAL", Set.of(Step.VALIDATE_INTERNAL)),
                arguments("NORMALIZE", Set.of(Step.NORMALIZE)),
                arguments("ENRICH", Set.of(Step.ENRICH)),
                arguments("MEDIA_PROCESS", Set.of(Step.MEDIA_PROCESS)),
                arguments("PUBLISH", Set.of(Step.PUBLISH)),
                arguments("CLOSE", Set.of(Step.CLOSE)),
                arguments("NON_SENSE", Set.of())
        );
    }

    @BeforeEach
    void prepare() {
        reset(errorLogRepository);
        reset(recordLogRepository);
    }

    @Test
    void logRecord_expectSuccess() {
        var record = Record.builder().recordId(1L).content("".getBytes()).datasetId("1")
                .language(Language.IT).country(Country.ITALY).datasetName("").build();
        var recordError = new RecordError("message", "stack");

        var event = new RecordProcessEvent(new RecordInfo(record), Step.HARVEST_ZIP, Status.SUCCESS);

        service.logRecordEvent(event);

        verify(recordLogRepository).save(any(RecordLogEntity.class));
        verify(errorLogRepository).saveAll(anyList());
    }

    @Test
    void logRecord_nullRecord_expectFail() {
        assertThrows(NullPointerException.class, () -> service.logRecordEvent(null));
    }

    @Test
    void logRecord_unableToSaveRecord_expectFail() {
        var record = Record.builder().recordId(1L).content("".getBytes()).datasetId("1")
                .language(Language.IT).country(Country.ITALY).datasetName("").build();

        var event = new RecordProcessEvent(new RecordInfo(record), Step.HARVEST_ZIP, Status.SUCCESS);

        when(recordLogRepository.save(any(RecordLogEntity.class)))
                .thenThrow(new RuntimeException("Exception saving"));

        assertThrows(ServiceException.class, () -> service.logRecordEvent(event));
    }

    @Test
    void logRecord_unableToSaveRecordErrors_expectFail() {
        var record = Record.builder().recordId(1L).content("".getBytes()).datasetId("1")
                .language(Language.IT).country(Country.ITALY).datasetName("").build();

        var event = new RecordProcessEvent(new RecordInfo(record), Step.HARVEST_ZIP, Status.SUCCESS);

        when(errorLogRepository.saveAll(anyList()))
                .thenThrow(new RuntimeException("Exception saving"));

        assertThrows(ServiceException.class, () -> service.logRecordEvent(event));
    }

    @Test
    void remove_expectSuccess() {
        service.remove("1");
        verify(errorLogRepository).deleteByRecordIdDatasetId("1");
        verify(recordLogRepository).deleteByRecordIdDatasetId("1");
    }

    @Test
    void remove_errorOnDelete_expectFail() {
        doThrow(new ServiceException("Failed", new Exception())).when(recordLogRepository)
                .deleteByRecordIdDatasetId("1");
        assertThrows(ServiceException.class, () -> service.remove("1"));
    }

    @Test
    void remove_nullInput_expectFail() {
        assertThrows(NullPointerException.class, () -> service.remove(null));
    }

    @ParameterizedTest
    @MethodSource("steps")
    void getProviderRecordStringByStep_expectSuccess(String stepName, Set<Step> expectedSteps) throws Exception {
        final RecordLogEntity recordLogEntity = new RecordLogEntity();
        recordLogEntity.setContent("content"+service.getSetFromStep(stepName));
        when(recordLogRepository.findRecordLogByRecordIdDatasetIdAndStepIn("recordId", "datasetId",
                service.getSetFromStep(stepName))).thenReturn(Set.of(recordLogEntity));
        final String providerRecord = service.getProviderRecordString("recordId", "datasetId", stepName);
        assertNotNull(providerRecord);
        assertEquals("content"+expectedSteps.toString(), providerRecord);
    }

    @ParameterizedTest
    @MethodSource("steps")
    void getSteps_expectSuccess(String stepName, Set<Step> expectedSteps) {
        assertEquals(expectedSteps, service.getSetFromStep(stepName));
    }

    @Test
    void getProviderRecordString_expectFail() {
        //Case null entity
        when(recordLogRepository.findRecordLogByRecordIdDatasetIdAndStepIn("recordId", "datasetId",
                Set.of(Step.HARVEST_OAI_PMH, Step.HARVEST_ZIP))).thenReturn(Collections.emptySet());
        assertThrows(NoRecordFoundException.class,
                () -> service.getProviderRecordString("recordId", "datasetId", null));

        //Case null content
        when(recordLogRepository.findRecordLogByRecordIdDatasetIdAndStepIn("recordId", "datasetId",
                Set.of(Step.HARVEST_OAI_PMH, Step.HARVEST_ZIP))).thenReturn(Set.of(new RecordLogEntity()));
        assertThrows(NoRecordFoundException.class,
                () -> service.getProviderRecordString("recordId", "datasetId", null));
    }

    @Test
    void getRecordLogEntity() {
        service.getRecordLogEntity("recordId", "datasetId", Step.MEDIA_PROCESS);
        verify(recordLogRepository).findRecordLogByRecordIdDatasetIdAndStep("recordId", "datasetId",
                Step.MEDIA_PROCESS);
        clearInvocations(recordLogRepository);

        //Case PROVIDER_ID
        service.getRecordLogEntity("recordId", "datasetId", Step.MEDIA_PROCESS);
        verify(recordLogRepository).findRecordLogByRecordIdDatasetIdAndStep("recordId", "datasetId",
                Step.MEDIA_PROCESS);
    }

}
